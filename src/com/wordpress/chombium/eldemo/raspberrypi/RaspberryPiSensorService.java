package com.wordpress.chombium.eldemo.raspberrypi;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.RaspiPin;
import com.wordpress.chombium.eldemo.sensors.SensorChangedListener;
import com.wordpress.chombium.eldemo.sensors.SensorService;

public class RaspberryPiSensorService implements ConfigurableComponent, SensorService {
    private static final Logger s_logger = LoggerFactory.getLogger(RaspberryPiSensorService.class);
    private static final String APP_ID = "Eclipsecon2015-eldemo-raspberrypi";
    private Map<String, Object> properties;
    
    private static final String VOLTAGE_READ_INTERVAL_PROP_NAME = "voltage.read.rate";
    
    private List<SensorChangedListener> _listeners = new CopyOnWriteArrayList<SensorChangedListener>();
    private GpioController _gpioController;
    private GpioPinDigitalMultipurpose _circuitBreakerSwitch;
    
	private ScheduledThreadPoolExecutor _scheduledThreadPoolExecutor;
	private ScheduledFuture<?> _handle;

    
    protected void activate(ComponentContext componentContext) {
    	try {
    		_gpioController = GpioFactory.getInstance();
    		
    		_circuitBreakerSwitch = _gpioController.provisionDigitalMultipurposePin(
					RaspiPin.GPIO_00, "circuitBreakerSwitch", PinMode.DIGITAL_OUTPUT);
    		_circuitBreakerSwitch.setShutdownOptions(true);
    		
    		s_logger.info("Bundle " + APP_ID + " has started!");
		} catch (Exception e) {
			s_logger.info("Could not activate Bundle " + APP_ID + "! Error: " + e.getMessage());
		}
    	
    }
    
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Bundle " + APP_ID + " has started with config!");
        updated(properties);
    }
    
    protected void deactivate(ComponentContext componentContext) {
    	s_logger.info("Deactivating " + APP_ID + "...");

    	if (_gpioController != null) {
			s_logger.info("... unexport all GPIOs");
			_gpioController.unexportAll();
			s_logger.info("... shutdown");
			_gpioController.shutdown();
			s_logger.info("... DONE.");
		}

		if (_handle != null) {
			_handle.cancel(true);
		}
    	
		s_logger.info("Deactivating " + APP_ID + " DONE.");
    }
    
    public void updated(Map<String, Object> properties) {
        this.properties = properties;
        if(properties != null && !properties.isEmpty()) {
            Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                s_logger.info("New property - " + entry.getKey() + " = " +
                entry.getValue() + " of type " + entry.getValue().getClass().toString());
            }
        }
    }
    
	@Override
	public Object getSensorValue(String sensorName)
			throws NoSuchSensorOrActuatorException {
		s_logger.info(APP_ID + "getSensorValue: {}", sensorName);
		if ("temperature".equals(sensorName)) {
			try {
				return readVoltage();
			} catch (IOException e) {
				return new NoSuchSensorOrActuatorException();
			}
		} else if ("circuitBreaker".equals(sensorName)) {
			return readCircuitBreakerSwitchState();
		} else
			throw new SensorService.NoSuchSensorOrActuatorException();
	}
	
	private synchronized float readVoltage() throws IOException{
		return 0.0F; //TODO Implemement me
	}
	
	private boolean readCircuitBreakerSwitchState() {
		return _circuitBreakerSwitch.getState().isHigh();
	}

	@Override
	public void setActuatorValue(String actuatorName, Object value)
			throws NoSuchSensorOrActuatorException {
		s_logger.info(APP_ID + "Set Actuator Value for: {} - {}", actuatorName, (String)value);

		if ("circuitBreaker".equals(actuatorName)) {
			_circuitBreakerSwitch.setState("on".equals(value));
			notifyListeners("circuitBreaker", value);
		} else {
			throw new SensorService.NoSuchSensorOrActuatorException();
		}	}

	private void notifyListeners(String sensorName, Object newValue) {
		for (SensorChangedListener listener : _listeners) {
			s_logger.info(APP_ID + "Notify listeners: {}", listener.getClass().getName());
			listener.sensorChanged(sensorName, newValue);
		}
	}
	
	public void addSensorChangedListener(SensorChangedListener listener) {
		s_logger.info(APP_ID + "add Changed listeners: {}", listener.getClass().getName());
		_listeners.add(listener);
	}

	public void removeSensorChangedListener(SensorChangedListener listener) {
		s_logger.info(APP_ID + "remove Changed listeners: {}", listener.getClass().getName());
		_listeners.remove(listener);
	}
}
