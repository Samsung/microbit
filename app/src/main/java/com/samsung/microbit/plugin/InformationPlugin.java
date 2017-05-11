package com.samsung.microbit.plugin;

import android.support.annotation.IntDef;
import android.util.Log;

import com.samsung.microbit.data.constants.RegistrationIds;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.presentation.BatteryPresenter;
import com.samsung.microbit.presentation.OrientationChangedPresenter;
import com.samsung.microbit.presentation.Presenter;
import com.samsung.microbit.presentation.ScreenOnOffPresenter;
import com.samsung.microbit.presentation.ShakePresenter;
import com.samsung.microbit.presentation.SignalStrengthPresenter;
import com.samsung.microbit.presentation.TemperaturePresenter;
import com.samsung.microbit.utils.ServiceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.samsung.microbit.plugin.InformationPlugin.AlertType.TYPE_BATTERY;
import static com.samsung.microbit.plugin.InformationPlugin.AlertType.TYPE_ORIENTATION;
import static com.samsung.microbit.plugin.InformationPlugin.AlertType.TYPE_SCREEN_ON_OFF;
import static com.samsung.microbit.plugin.InformationPlugin.AlertType.TYPE_SHAKE;
import static com.samsung.microbit.plugin.InformationPlugin.AlertType.TYPE_SIGNAL_STRENGTH;
import static com.samsung.microbit.plugin.InformationPlugin.AlertType.TYPE_TEMPERATURE;

/**
 * Provide ability to collect and share device information.
 */
public class InformationPlugin implements AbstractPlugin {
    private static final String TAG = InformationPlugin.class.getSimpleName();

    private List<Presenter> activePresenters = new ArrayList<>();
    private List<Integer> alertTypes = new ArrayList<>();

    @Override
    public void handleEntry(CmdArg cmd) {
        boolean register = false;
        if(cmd.getValue() != null) {
            register = cmd.getValue().toLowerCase().equals("on");
        }

        switch(cmd.getCMD()) {
            case RegistrationIds.REG_SIGNALSTRENGTH:
                if(register) {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_SIGNAL_STRENGTH);
                    if(presenter == null) {
                        SignalStrengthPresenter signalStrengthPresenter = new SignalStrengthPresenter();
                        signalStrengthPresenter.setInformationPlugin(this);
                        activePresenters.add(signalStrengthPresenter);
                        alertTypes.add(AlertType.TYPE_SIGNAL_STRENGTH);

                        presenter = signalStrengthPresenter;
                    }

                    presenter.start();
                } else {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_SIGNAL_STRENGTH);
                    if(presenter != null) {
                        presenter.stop();
                    }
                }
                break;

            case RegistrationIds.REG_DEVICEORIENTATION:
                if(register) {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_ORIENTATION);
                    if(presenter == null) {
                        OrientationChangedPresenter orientationChangedPresenter = new OrientationChangedPresenter();
                        activePresenters.add(orientationChangedPresenter);
                        alertTypes.add(AlertType.TYPE_ORIENTATION);

                        presenter = orientationChangedPresenter;
                    }

                    presenter.start();
                } else {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_ORIENTATION);
                    if(presenter != null) {
                        presenter.stop();
                    }
                }
                break;

            case RegistrationIds.REG_DEVICEGESTURE:
                if(register) {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_SHAKE);
                    if(presenter == null) {
                        ShakePresenter shakePresenter = new ShakePresenter();
                        shakePresenter.setInformationPlugin(this);
                        activePresenters.add(shakePresenter);
                        alertTypes.add(AlertType.TYPE_SHAKE);

                        presenter = shakePresenter;
                    }

                    presenter.start();
                } else {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_SHAKE);
                    if(presenter != null) {
                        presenter.stop();
                    }
                }
                break;

            case RegistrationIds.REG_BATTERYSTRENGTH:
                if(register) {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_BATTERY);
                    if(presenter == null) {
                        BatteryPresenter batteryPresenter = new BatteryPresenter();
                        batteryPresenter.setInformationPlugin(this);
                        activePresenters.add(batteryPresenter);
                        alertTypes.add(AlertType.TYPE_BATTERY);

                        presenter = batteryPresenter;
                    }

                    presenter.start();
                } else {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_BATTERY);
                    if(presenter != null) {
                        presenter.stop();
                    }
                }
                break;

            case RegistrationIds.REG_TEMPERATURE:
                if(register) {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_TEMPERATURE);
                    if(presenter == null) {
                        TemperaturePresenter temperaturePresenter = new TemperaturePresenter();
                        temperaturePresenter.setInformationPlugin(this);
                        activePresenters.add(temperaturePresenter);
                        alertTypes.add(AlertType.TYPE_TEMPERATURE);

                        presenter = temperaturePresenter;
                    }

                    presenter.start();
                } else {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_TEMPERATURE);
                    if(presenter != null) {
                        presenter.stop();
                    }
                }
                break;
            case RegistrationIds.REG_DISPLAY:
                if(register) {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_SCREEN_ON_OFF);
                    if(presenter == null) {
                        ScreenOnOffPresenter screenOnOffPresenter = new ScreenOnOffPresenter();
                        activePresenters.add(screenOnOffPresenter);
                        alertTypes.add(AlertType.TYPE_SCREEN_ON_OFF);

                        presenter = screenOnOffPresenter;
                    }

                    presenter.start();
                } else {
                    Presenter presenter = findPresenterByType(AlertType.TYPE_SCREEN_ON_OFF);
                    if(presenter != null) {
                        presenter.stop();
                    }
                }
                break;
            default:
                Log.e(TAG, "Unknown category");
        }
    }

    public void sendReplyCommand(int mbsService, CmdArg cmd) {
        // TODO not needed ??? remove
        ServiceUtils.sendReplyCommand(mbsService, cmd);
    }

    private Presenter findPresenterByType(@AlertType int alertType) {
        int index = alertTypes.indexOf(alertType);

        if(index != -1) {
            return activePresenters.get(index);
        } else {
            return null;
        }
    }

    @Override
    public void destroy() {
        for(Presenter presenter : activePresenters) {
            presenter.stop();
            presenter.destroy();
        }
        activePresenters.clear();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @IntDef(value = {TYPE_ORIENTATION, TYPE_SHAKE, TYPE_BATTERY, TYPE_SIGNAL_STRENGTH, TYPE_TEMPERATURE,
            TYPE_SCREEN_ON_OFF})
    public @interface AlertType {
        int TYPE_ORIENTATION = 0;
        int TYPE_SHAKE = 1;
        int TYPE_BATTERY = 2;
        int TYPE_SIGNAL_STRENGTH = 3;
        int TYPE_TEMPERATURE = 4;
        int TYPE_SCREEN_ON_OFF = 5;
    }
}
