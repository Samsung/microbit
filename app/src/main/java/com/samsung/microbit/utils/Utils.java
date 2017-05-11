package com.samsung.microbit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.Project;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Utility that contains common functionality that
 * uses along the app.
 */
public class Utils {
    public final static int SORTBY_PROJECT_DATE = 0;
    public final static int SORTBY_PROJECT_NAME = 1;
    public final static int ORDERBY_ASCENDING = 0;
    public final static int ORDERBY_DESCENDING = 1;

    private Utils() {
    }

    /**
     * Allows to sort the list of projects on the Flash screen according
     * to given parameters. It allows to sort by date or name, for now, and
     * set sort order to ascending or descending.
     *
     * @param list      List to sort.
     * @param orderBy   Defines sorting criteria of a project by which to sort.
     * @param sortOrder Ascending or descending.
     * @return Sorted list.
     */
    public static List<Project> sortProjectList(List<Project> list, final int orderBy, final int sortOrder) {
        Project[] projectArray = list.toArray(new Project[list.size()]);
        Comparator<Project> comparator = new Comparator<Project>() {
            @Override
            public int compare(Project lhs, Project rhs) {
                int rc;
                switch(orderBy) {

                    case SORTBY_PROJECT_DATE:
                        // byTimestamp
                        if(lhs.timestamp < rhs.timestamp) {
                            rc = 1;
                        } else if(lhs.timestamp > rhs.timestamp) {
                            rc = -1;
                        } else {
                            rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                        }

                        break;
                    default:
                        // byName
                        rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                        break;
                }

                if(sortOrder != ORDERBY_ASCENDING) {
                    rc = 0 - rc;
                }

                return rc;
            }
        };

        Arrays.sort(projectArray, comparator);
        list.clear();
        list.addAll(Arrays.asList(projectArray));
        return list;
    }

    /**
     * Going to and coming from microbit the following rule applies:
     * low 16 bits == event category
     * (e.g. {@link EventCategories#SAMSUNG_REMOTE_CONTROL_ID),
     * high 16 bits ==  event sub code
     * (eg. {@link EventSubCodes#SAMSUNG_REMOTE_CONTROL_EVT_PLAY)
     */
    public static int makeMicroBitValue(int category, int value) {
        return ((value << 16) | category);
    }

    public static void unbindDrawables(View view) {
        if(view == null) {
            return;
        }

        if(view.getBackground() != null) {
            Drawable backgroundDrawable = view.getBackground();
            backgroundDrawable.setCallback(null);
            view.unscheduleDrawable(backgroundDrawable);

            if(backgroundDrawable instanceof GifDrawable) {
                ((GifDrawable) backgroundDrawable).recycle();
            }
        }

        if(view instanceof ImageView && ((ImageView) view).getDrawable() != null) {
            Drawable backgroundDrawable = ((ImageView) view).getDrawable();
            backgroundDrawable.setCallback(null);
            view.unscheduleDrawable(backgroundDrawable);

            if(backgroundDrawable instanceof GifDrawable) {
                ((GifDrawable) backgroundDrawable).recycle();
            }
        }

        if(view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for(int i = 0; i < viewGroup.getChildCount(); i++) {
                unbindDrawables(viewGroup.getChildAt(i));
            }
        }
    }

    /**
     * Allows to get a list sort order from shared preferences, for example, to sort
     * the list of projects on the Flash screen. If nothing found then
     * it return the default value - 0.
     *
     * @return Sort order.
     */
    public static int getListSortOrder() {
        SharedPreferences prefs = MBApp.getApp().getSharedPreferences(Constants.PREFERENCES, Context
                .MODE_MULTI_PROCESS);

        int i = 0;
        if(prefs != null) {
            i = prefs.getInt(Constants.PREFERENCES_LIST_ORDER, 0);
        }

        return i;
    }

    /**
     * Allows to save current sort order value to shared preferences.
     *
     * @param listSortOrder Current sort order value.
     */
    public static void setListSortOrder(int listSortOrder) {
        SharedPreferences prefs = MBApp.getApp().getSharedPreferences(Constants.PREFERENCES, Context
                .MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.PREFERENCES_LIST_ORDER, listSortOrder);
        editor.apply();
    }

    public static UUID makeUUID(String baseUUID, long shortUUID) {
        UUID u = UUID.fromString(baseUUID);
        long msb = u.getMostSignificantBits();
        long mask = 0x0ffffL;
        shortUUID &= mask;
        msb &= ~(mask << 32);
        msb |= (shortUUID << 32);
        u = new UUID(msb, u.getLeastSignificantBits());
        return u;
    }
}
