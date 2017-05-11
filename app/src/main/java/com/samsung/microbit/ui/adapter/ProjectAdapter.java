package com.samsung.microbit.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;
import com.samsung.microbit.data.model.Project;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.activity.ProjectActivity;
import com.samsung.microbit.ui.control.ExtendedEditText;
import com.samsung.microbit.utils.FileUtils;

import java.util.List;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Represents a project adapter that allows to custom view for
 * a single project item.
 */
public class ProjectAdapter extends BaseAdapter {
    private static final String TAG = ProjectAdapter.class.getSimpleName();

    private List<Project> mProjects;
    private ProjectActivity mProjectActivity;
    int currentEditableRow = -1;

    private static final int FOCUS_DELAY = 300;

    private InputFilter renameFilter = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            for(int i = start; i < end; i++) {
                if(!Character.isLetterOrDigit(source.charAt(i)) && !isAdditionalAllowedSymbol(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        }

        private boolean isAdditionalAllowedSymbol(char value) {
            return value == '-' || value == '_' || value == ' ';
        }
    };

    /**
     * Simplified method to log informational messages.
     * Uses in Debug mode only.
     *
     * @param message Message to log.
     */
    protected void logi(String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    /**
     * Listener for some editor's actions. If editing is done then
     * hide the keyboard and rename the project. If canceled then just
     * hide the keyboard.
     */
    private TextView.OnEditorActionListener editorOnActionListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            logi("onEditorAction() :: currentEditableRow=" + currentEditableRow);

            int pos = (int) v.getTag(R.id.positionId);
            Project project = mProjects.get(pos);
            project.inEditMode = false;
            currentEditableRow = -1;

            if(actionId == EditorInfo.IME_ACTION_DONE) {
                dismissKeyBoard(v, true, true);
            } else if(actionId == -1) {
                dismissKeyBoard(v, true, false);
            }

            return true;
        }
    };

    /**
     * On click listener for a project item. Allows to expand or shrink
     * project item view if it's in expand mode. If not, click provides
     * confirmation to rename the project.
     */
    private View.OnClickListener appNameClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("OnClickListener() :: " + v.getClass().getName());

            boolean expandProjectItem;

            expandProjectItem = mProjectActivity.getResources().getBoolean(R.bool.expandProjectItem);

            if(expandProjectItem) {
                changeActionBar(v);
            } else {
                if(currentEditableRow != -1) {
                    int i = (Integer) v.getTag(R.id.positionId);
                    if(i != currentEditableRow) {
                        renameProject(v);
                    }
                } else {
                    renameProject(v);
                }
            }
        }
    };

    /**
     * On long click listener that provides rename action.
     */
    private View.OnLongClickListener appNameLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {

            logi("OnLongClickListener() :: " + v.getClass().getName());
            renameProject(v);
            return true;
        }
    };

    /**
     * Sets editTextView invisible and project button visible.
     *
     * @param v Edit text view.
     */
    private void hideEditTextView(View v) {
        Button bt = (Button) v.getTag(R.id.editbutton);
        bt.setVisibility(View.VISIBLE);
        v.setVisibility(View.INVISIBLE);
    }

    /**
     * Sets editTextView visible and project button invisible.
     *
     * @param v Edit text view.
     */
    private void showEditTextView(View v) {
        Button bt = (Button) v.getTag(R.id.editbutton);
        bt.setVisibility(View.INVISIBLE);
        v.setVisibility(View.VISIBLE);
    }

    /**
     * Allows to hide keyboard and rename a project file by given view
     * from the list of projects.
     *
     * @param v    View that represents a project from the list.
     * @param hide If true - hides the keyboard.
     * @param done If true - renames given project file.
     */
    private void dismissKeyBoard(View v, boolean hide, boolean done) {
        logi("dismissKeyBoard() :: ");

        int pos = (Integer) v.getTag(R.id.positionId);

        logi("dismissKeyBoard() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        InputMethodManager imm = (InputMethodManager) mProjectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);

        if(hide) {
            hideEditTextView(v);
        }

        if(done) {
            EditText ed = (EditText) v;
            pos = (int) ed.getTag(R.id.positionId);
            String newName = ed.getText().toString();
            Project p = mProjects.get(pos);
            if(newName.length() > 0) {
                if(p.name.compareToIgnoreCase(newName) != 0) {
                    mProjectActivity.renameFile(p.filePath, newName);
                }
            }
        }
    }

    /**
     * Sets project item in edit mode and shows keyboard to edit project name.
     *
     * @param v Edit text view.
     */
    private void showKeyBoard(final View v) {
        logi("showKeyBoard() :: " + v.getClass().getName());

        int pos = (Integer) v.getTag(R.id.positionId);

        logi("showKeyBoard() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        showEditTextView(v);

        final InputMethodManager imm = (InputMethodManager) mProjectActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                imm.showSoftInput(v, 0);
            }
        }, 0);

        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.requestFocus();
            }
        }, FOCUS_DELAY);
    }

    /**
     * Changes project item state from expanded to not expanded and visa versa.
     * If it is in edit mode then it additionally closes edit mode and hides keyboard.
     *
     * @param v Project item view.
     */
    private void changeActionBar(View v) {
        logi("changeActionBar() :: ");

        int pos = (int) v.getTag(R.id.positionId);
        logi("changeActionBar() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        Project project = mProjects.get(pos);
        project.actionBarExpanded = !project.actionBarExpanded;
        if(currentEditableRow != -1) {
            project = mProjects.get(currentEditableRow);
            project.inEditMode = false;
            currentEditableRow = -1;
            dismissKeyBoard(v, false, false);
        }

        notifyDataSetChanged();
    }

    /**
     * Renames a project item by given view. View contains reference to editTextView
     * from where it gets a new project name.
     *
     * @param v Edit text view.
     */
    private void renameProject(View v) {
        logi("renameProject() :: ");

        int pos = (int) v.getTag(R.id.positionId);
        logi("renameProject() :: pos = " + pos + " currentEditableRow=" + currentEditableRow);

        Project project;
        if(currentEditableRow != -1) {
            project = mProjects.get(currentEditableRow);
            project.inEditMode = false;
            currentEditableRow = -1;
        }

        project = mProjects.get(pos);
        project.inEditMode = !project.inEditMode;
        currentEditableRow = pos;
        View ev = (View) v.getTag(R.id.textEdit);
        showKeyBoard(ev);
        notifyDataSetChanged();
    }

    /**
     * Occurs when a user clicks on the Flash button on some project item.
     * Sends clicked project to flash to a micro:bit board.
     */
    private View.OnClickListener sendBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            logi("sendBtnClickListener() :: ");
            mProjectActivity.sendProject((Project) ProjectAdapter.this.getItem((Integer) v.getTag()));
        }
    };

    /**
     * Occurs when a user clicks on the Delete button on some project item.
     * Shows a dialog window to confirm deletion.
     */
    private View.OnClickListener deleteBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("deleteBtnClickListener() :: ");
            final int pos = (int) v.getTag();
            //Update Stats

            GoogleAnalyticsManager.getInstance()
                    .sendNavigationStats(ProjectActivity.class.getSimpleName(), "DeleteProject");

            PopUp.show(mProjectActivity.getString(R.string.delete_project_message),
                    mProjectActivity.getString(R.string.delete_project_title),
                    R.drawable.ic_trash, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_NONE,
                    PopUp.TYPE_CHOICE,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PopUp.hide();
                            Project proj = mProjects.get(pos);
                            if(FileUtils.deleteFile(proj.filePath)) {
                                mProjects.remove(pos);
                                notifyDataSetChanged();
                                mProjectActivity.updateProjectsListSortOrder(true);
                            }
                        }
                    }, null);
        }
    };

    public ProjectAdapter(ProjectActivity projectActivity, List<Project> list) {
        this.mProjectActivity = projectActivity;
        mProjects = list;
    }

    @Override
    public int getCount() {
        return mProjects.size();
    }

    @Override
    public Object getItem(int position) {
        return mProjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Project project = mProjects.get(position);
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(MBApp.getApp());
            convertView = inflater.inflate(R.layout.project_items, null);
        }

        Button appNameButton = (Button) convertView.findViewById(R.id.appNameButton);
        appNameButton.setTypeface(MBApp.getApp().getRobotoTypeface());

        ExtendedEditText appNameEdit = (ExtendedEditText) convertView.findViewById(R.id.appNameEdit);
        appNameEdit.setTypeface(MBApp.getApp().getRobotoTypeface());

        LinearLayout actionBarLayout = (LinearLayout) convertView.findViewById(R.id.actionBarForProgram);
        if(actionBarLayout != null) {
            if(project.actionBarExpanded) {
                actionBarLayout.setVisibility(View.VISIBLE);
                appNameButton.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(MBApp.getApp()
                        , R.drawable.ic_arrow_down), null);
            } else {
                actionBarLayout.setVisibility(View.GONE);
                appNameButton.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(MBApp.getApp()
                        , R.drawable.ic_arrow_left), null);
            }
        }

        appNameButton.setText(project.name);
        appNameButton.setTag(R.id.positionId, position);
        appNameButton.setTag(R.id.textEdit, appNameEdit);
        appNameButton.setOnClickListener(appNameClickListener);
        appNameButton.setOnLongClickListener(appNameLongClickListener);

        appNameEdit.setTag(R.id.positionId, position);
        appNameEdit.setTag(R.id.editbutton, appNameButton);
        appNameEdit.setOnEditorActionListener(editorOnActionListener);
        appNameEdit.setFilters(new InputFilter[]{renameFilter});

        if(project.inEditMode) {
            appNameEdit.setVisibility(View.VISIBLE);

            appNameEdit.setText(project.name);
            appNameEdit.setSelection(project.name.length());
            appNameEdit.requestFocus();
            appNameButton.setVisibility(View.INVISIBLE);

        } else {
            appNameEdit.setVisibility(View.INVISIBLE);
            appNameButton.setVisibility(View.VISIBLE);
            //dismissKeyBoard(appNameEdit, false);
        }

        //appNameEdit.setOnClickListener(appNameClickListener);

        TextView flashBtnText = (TextView) convertView.findViewById(R.id.project_item_text);
        flashBtnText.setTypeface(MBApp.getApp().getRobotoTypeface());
        LinearLayout sendBtnLayout = (LinearLayout) convertView.findViewById(R.id.sendBtn);
        sendBtnLayout.setTag(position);
        sendBtnLayout.setOnClickListener(sendBtnClickListener);

        ImageButton deleteBtn = (ImageButton) convertView.findViewById(R.id.deleteBtn);
        deleteBtn.setTag(position);
        deleteBtn.setOnClickListener(deleteBtnClickListener);
        deleteBtn.setEnabled(true);


        Drawable myIcon;
        if(project.runStatus) {
            flashBtnText.setText("");
            myIcon = convertView.getResources().getDrawable(R.drawable.green_btn);
        } else {
            flashBtnText.setText(R.string.flash);
            myIcon = convertView.getResources().getDrawable(R.drawable.blue_btn);
        }
        sendBtnLayout.setBackground(myIcon);

        sendBtnLayout.setClickable(true);
        return convertView;
    }
}
