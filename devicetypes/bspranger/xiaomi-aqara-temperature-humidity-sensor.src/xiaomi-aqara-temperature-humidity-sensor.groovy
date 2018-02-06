/**
 *  Xiaomi Aqara Temperature Humidity Sensor
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  2017-03 First release of the Xiaomi Temp/Humidity Device Handler
 *  2017-03 Includes battery level (hope it works, I've only had access to a device for a limited period, time will tell!)
 *  2017-03 Last checkin activity to help monitor health of device and multiattribute tile
 *  2017-03 Changed temperature to update on .1° changes - much more useful
 *  2017-03-08 Changed the way the battery level is being measured. Very different to other Xiaomi sensors.
 *  2017-03-23 Added Fahrenheit support
 *  2017-03-25 Minor update to display unknown battery as "--", added fahrenheit colours to main and device tiles
 *  2017-03-29 Temperature offset preference added to handler
 *
 *  known issue: these devices do not seem to respond to refresh requests left in place in case things change
 *  known issue: tile formatting on ios and android devices vary a little due to smartthings app - again, nothing I can do about this
 *  known issue: there's nothing I can do about the pairing process with smartthings. it is indeed non standard, please refer to community forum for details
 *
 *  Change log:
 *  bspranger - renamed to bspranger to remove confusion of a4refillpad
 */

metadata {
	definition (name: "Xiaomi Aqara Temperature Humidity Sensor", namespace: "bspranger", author: "bspranger") {
	capability "Temperature Measurement"
	capability "Relative Humidity Measurement"
	capability "Sensor"
	capability "Battery"
	capability "Health Check"

	attribute "lastCheckin", "String"
	attribute "lastCheckinDate", "String"
	attribute "maxTemp", "number"
	attribute "minTemp", "number"
	attribute "maxHumidity", "number"
	attribute "minHumidity", "number"
	attribute "currentHumidity", "number"
	attribute "multiAttributesReport", "String"
	attribute "multiAttributesIcon", "String"
	attribute "currentDay", "String"
	attribute "batteryRuntime", "String"

	fingerprint profileId: "0104", deviceId: "5F01", inClusters: "0000, 0003, FFFF, 0402, 0403, 0405", outClusters: "0000, 0004, FFFF", manufacturer: "LUMI", model: "lumi.weather", deviceJoinName: "Xiaomi Aqara Temp Sensor"

	command "resetBatteryRuntime"
	command "tempReset"
	}

    // simulator metadata
    simulator {
        for (int i = 0; i <= 100; i += 10) {
            status "${i}F": "temperature: $i F"
        }

        for (int i = 0; i <= 100; i += 10) {
            status "${i}%": "humidity: ${i}%"
        }
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"temperature", type:"generic", width:6, height:4) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("temperature", label:'${currentValue}°',
                    backgroundColors:[
                        [value: 0, color: "#153591"],
                        [value: 5, color: "#1e9cbb"],
                        [value: 10, color: "#90d2a7"],
                        [value: 15, color: "#44b621"],
                        [value: 20, color: "#f1d801"],
                        [value: 25, color: "#d04e00"],
                        [value: 30, color: "#bc2323"],
                        [value: 44, color: "#1e9cbb"],
                        [value: 59, color: "#90d2a7"],
                        [value: 74, color: "#44b621"],
                        [value: 84, color: "#f1d801"],
                        [value: 95, color: "#d04e00"],
                        [value: 96, color: "#bc2323"]
                    ]
                )
            }
            tileAttribute("device.multiAttributesReport", key: "SECONDARY_CONTROL") {
                attributeState("multiAttributesReport", label:'${currentValue}')
            }
        }
        valueTile("temperature2", "device.temperature", inactiveLabel: false) {
            state "temperature", label:'${currentValue}°', icon:"st.Weather.weather2",
            backgroundColors:[
                [value: 0, color: "#153591"],
                [value: 5, color: "#1e9cbb"],
                [value: 10, color: "#90d2a7"],
                [value: 15, color: "#44b621"],
                [value: 20, color: "#f1d801"],
                [value: 25, color: "#d04e00"],
                [value: 30, color: "#bc2323"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
            state "humidity", label:'${currentValue}%', unit:"%", icon:"st.Weather.weather12",
            backgroundColors:[
                [value: 0, color: "#FFFCDF"],
                [value: 4, color: "#FDF789"],
                [value: 20, color: "#A5CF63"],
                [value: 23, color: "#6FBD7F"],
                [value: 56, color: "#4CA98C"],
                [value: 59, color: "#0072BB"],
                [value: 76, color: "#085396"]
            ]
        }
        standardTile("pressure", "device.pressure", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "pressure", label:'${currentValue}', icon:"st.Weather.weather1"
        }
        valueTile("battery", "device.battery", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}%', unit:"%",
            backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
            ]
        }
        valueTile("lastcheckin", "device.lastCheckin", inactiveLabel: false, decoration:"flat", width: 4, height: 1) {
            state "lastcheckin", label:'Last Checkin:\n ${currentValue}'
        }
        valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration:"flat", width: 4, height: 1) {
            state "batteryRuntime", label:'Battery Changed:\n ${currentValue}'
        }

        main("temperature2")
        details(["temperature", "battery", "humidity", "pressure", "lastcheckin", "batteryRuntime"])
    }
    preferences {
        section {
            input description: "The settings below customize additional infomation displayed in the main status tile.", type: "paragraph", element: "paragraph", title: "MAIN TILE DISPLAY"
            input name: "displayTempInteger", type: "bool", title: "Display temperature as integer?", defaultValue: false
            input name: "displayTempHighLow", type: "bool", title: "Display high/low temperature?", defaultValue: false
            input name: "displayHumidHighLow", type: "bool", title: "Display high/low humidity?", defaultValue: false
        }
        section {
            input description: "The settings below allow correction of variations in temperature, humidity, and pressure by setting an offset. Examples: If the sensor consistently reports temperature 5 degrees too warm, enter '-5' for the Temperature Offset. If it reports humidity 3% too low, enter ‘3' for the Humidity Offset. NOTE: Changes will take effect on the NEXT temperature / humidity / pressure report.", type: "paragraph", element: "paragraph", title: "OFFSETS & UNITS"
            input "tempOffset", "number", title:"Temperature Offset", description:"Adjust temperature by this many degrees", range:"*..*"
            input "humidOffset", "number", title:"Humidity Offset", description:"Adjust humidity by this many percent", range: "*..*"
            input "pressOffset", "number", title:"Pressure Offset", description:"Adjust pressure by this many units", range: "*..*"
            input name:"PressureUnits", type:"enum", title:"Pressure Units", options:["mbar", "kPa", "inHg", "mmHg"], description:"Sets the unit in which pressure will be reported"
            input description: "NOTE: The temperature unit (C / F) can be changed in the location settings for your hub.", type: "paragraph", element: "paragraph", title: ""
        }
        section {
            input description: "", type: "paragraph", element: "paragraph", title: "DATE & CLOCK"    
            input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", options:["US","UK","Other"]
            input name: "clockformat", type: "bool", title: "Use 24 hour clock?", defaultValue: false
        }
        section {
            input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "CHANGED BATTERY DATE RESET"
            input name: "battReset", type: "bool", title: "Battery Changed?", description: ""
        }
        section {
            input description: "Only change the settings below if you know what you're doing.", type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
            input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts.\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3
            input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing)\nat __ volts.  Range 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5
        }
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "${device.displayName}: Parsing description: ${description}"

    // Send event for heartbeat
    def now = formatDate()    
    def nowDate = new Date(now).getTime()
    sendEvent(name: "lastCheckin", value: now, displayed: false)
    sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false)

	// Check if the min/max temps should be reset
    checkNewDay(now)

	// getEvent automatically retrieves temp and humidity in correct unit as integer
	Map map = zigbee.getEvent(description)

	if (map.name == "temperature") {
 		if (tempOffset) {
			map.value = (int) map.value + (int) tempOffset
		}
		map.descriptionText = "${device.displayName} temperature is ${map.value}${temperatureScale}°"
		map.translatable = true
		updateMinMaxTemps(map.value)
	} else if (map.name == "humidity") {
		if (humidityOffset) {
			map.value = (int) map.value + (int) humidityOffset
		}
		updateMinMaxHumidity(map.value)
	} else if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	} else if (description?.startsWith('read attr - raw:')) {
		map = parseReadAttr(description)
	} else {
		log.debug "${device.displayName}: was unable to parse ${description}"
        sendEvent(name: "lastCheckin", value: now)
	}

	if (map) {
		log.debug "${device.displayName}: Parse returned ${map}"
	}

	return map ? createEvent(map) : [:]
}

private Map parseCatchAllMessage(String description) {
    def i
    def cluster = zigbee.parse(description)
    log.debug cluster

    Map resultMap = [:]

    if (cluster) {
        switch(cluster.clusterId)
        {
            case 0x0000:
                def MsgLength = cluster.data.size();

                // Original Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
                if ((cluster.data.get(0) == 0x02) && (cluster.data.get(1) == 0xFF)) {
                    for (i = 0; i < (MsgLength-3); i++)
                    {
                        if (cluster.data.get(i) == 0x21) // check the data ID and data type
                        {
                            // next two bytes are the battery voltage.
                            resultMap = getBatteryResult((cluster.data.get(i+2)<<8) + cluster.data.get(i+1))
                            break
                        }
                    }
                } else if ((cluster.data.get(0) == 0x01) && (cluster.data.get(1) == 0xFF)) {
                    for (i = 0; i < (MsgLength-3); i++)
                    {
                        if ((cluster.data.get(i) == 0x01) && (cluster.data.get(i+1) == 0x21))  // check the data ID and data type
                        {
                            // next two bytes are the battery voltage.
                            resultMap = getBatteryResult((cluster.data.get(i+3)<<8) + cluster.data.get(i+2))
                            break
                        }
                    }
                }
            break
        }
    }
    return resultMap
}


// parseReadAttr handles pressure reports or battery report on reset button press
private Map parseReadAttr(String description) {
	Map resultMap = [:]

	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()

	// log.debug "${device.displayName}: Parsing read attr: cluster: ${cluster}, attrId: ${attrId}, value: ${value}"

	if ((cluster == "0403") && (attrId == "0000")) {
		def result = value[0..3]
		float pressureval = Integer.parseInt(result, 16)

		if (!(settings.PressureUnits)){
			settings.PressureUnits = "mbar"
		}
		// log.debug "${device.displayName}: Converting ${pressureval} to ${PressureUnits}"
	
		switch (PressureUnits) {
			case "mbar":
				pressureval = (pressureval/10) as Float
				pressureval = pressureval.round(1);
				break;

			case "kPa":
				pressureval = (pressureval/100) as Float
				pressureval = pressureval.round(2);
				break;

			case "inHg":
				pressureval = (((pressureval/10) as Float) * 0.0295300)
				pressureval = pressureval.round(2);
				break;

			case "mmHg":
				pressureval = (((pressureval/10) as Float) * 0.750062)
				pressureval = pressureval.round(2);
				break;
		}

		// log.debug "${device.displayName}: Pressure is ${pressureval} ${PressureUnits} before applying the pressure offset."

		if (settings.pressOffset) {
			pressureval = (pressureval + settings.pressOffset)
		}

		pressureval = pressureval.round(2);

		resultMap = [
			name: 'pressure',
			value: pressureval,
			unit: "${PressureUnits}",
			isStateChange: true,
			descriptionText : "${device.displayName} Pressure is ${pressureval} ${PressureUnits}"
		]
	} else if (cluster == "0000" && attrId == "0005")  {
		def modelName = ""
	
		// Parsing the model name
		for (int i = 0; i < value.length(); i+=2) {
			def str = value.substring(i, i+2);
			def NextChar = (char)Integer.parseInt(str, 16);
			modelName = modelName + NextChar
		}
		log.debug "${device.displayName}: Reported model: ${modelName}"
	}
	return resultMap
}

private Map getBatteryResult(rawValue) {
    def rawVolts = rawValue / 1000
    def minVolts
    def maxVolts

    if(voltsmin == null || voltsmin == "")
    	minVolts = 2.5
    else
   	minVolts = voltsmin
    
    if(voltsmax == null || voltsmax == "")
    	maxVolts = 3.0
    else
	maxVolts = voltsmax
    
    def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))

    def result = [
        name: 'battery',
        value: roundedPct,
        unit: "%",
        isStateChange: true,
        descriptionText : "${device.displayName} raw battery is ${rawVolts}v"
    ]

    return result
}

def resetBatteryRuntime() {
    def now = formatDate(true)   
    sendEvent(name: "batteryRuntime", value: now)
}

// If the day of month has changed from that of previous event, reset the daily min/max temp values
def checkNewDay(now) {
	def oldDay = ((device.currentValue("currentDay")) == null) ? "32" : (device.currentValue("currentDay"))
	def newDay = new Date(now).format("dd")
	// log.debug "${device.displayName}: currentDay = ${device.currentValue("currentDay")}, oldDay = ${oldDay}, newDay = ${newDay}"
	if (newDay != oldDay) {
		tempReset()
		sendEvent(name: "currentDay", value: newDay, displayed: false)
	}
}

// Reset daily min/max temp and humidity values to the current temp/humidity values
def tempReset() {
	def currentTemp = device.currentState('temperature')?.value
	log.debug "${device.displayName}: Resetting daily min/max temp values to current temperature of ${currentTemp}"
    sendEvent(name: "maxTemp", value: device.currentValue("temperature"), displayed: false)
    sendEvent(name: "minTemp", value: device.currentValue("temperature"), displayed: false)
    sendEvent(name: "maxHumidity", value: device.currentValue("humidity"), displayed: false)
    sendEvent(name: "minHumidity", value: device.currentValue("humidity"), displayed: false)
    refreshMultiAttributes()
}

// Check new min or max temp for the day
def updateMinMaxTemps(temp) {
	if ((temp > device.currentValue('maxTemp')) || (device.currentValue('maxTemp') == null))
		sendEvent(name: "maxTemp", value: temp, displayed: false)	
	if ((temp < device.currentValue('minTemp')) || (device.currentValue('minTemp') == null))
		sendEvent(name: "minTemp", value: temp, displayed: false)
	refreshMultiAttributes()
}

// Check new min or max humidity for the day and set new currentHumidity
def updateMinMaxHumidity(humidity) {
	if ((humidity > device.currentValue('maxHumidity')) || (device.currentValue('maxHumidity') == null))
		sendEvent(name: "maxHumidity", value: humidity, displayed: false)
	if ((humidity < device.currentValue('minHumidity')) || (device.currentValue('minHumidity') == null))
		sendEvent(name: "minHumidity", value: humidity, displayed: false)
	sendEvent(name: "currentHumidity", value: humidity, displayed: false)
	refreshMultiAttributes()
}

	// Update display of multiattributes in main tile
def refreshMultiAttributes() {
	def temphiloAttributes = displayTempHighLow ? (displayHumidHighLow ? "Today's High/Low:  ${device.currentState('maxTemp')?.value}° / ${device.currentState('minTemp')?.value}°" : "Today's High: ${device.currentState('maxTemp')?.value}°  /  Low: ${device.currentState('minTemp')?.value}°") : ""
	def humidhiloAttributes = displayHumidHighLow ? (displayTempHighLow ? "    ${device.currentState('maxHumidity')?.value}% / ${device.currentState('minHumidity')?.value}%" : "Today's High: ${device.currentState('maxHumidity')?.value}%  /  Low: ${device.currentState('minHumidity')?.value}%") : ""
    sendEvent(name: "multiAttributesReport", value: "${temphiloAttributes}${humidhiloAttributes}", displayed: false)
}

def configure() {
    log.debug "${device.displayName}: Configuring"
    state.battery = 0
    checkIntervalEvent("configured");
    return
}

def installed() {
    state.battery = 0
    resetBatteryRuntime()
    log.debug "${device.displayName}: Setting Battery Changed to current date for newly paired sensor"
    checkIntervalEvent("installed");
}

def updated() {
    checkIntervalEvent("updated");
	if(battReset){
		resetBatteryRuntime()
		device.updateSetting("battReset", false)
	}
	updateMinMaxTemps(device.currentValue('temperature'))
	updateMinMaxHumidity(device.currentValue('humidity'))
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when ${text}()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
    def correctedTimezone = ""
    def timeString = clockformat ? "HH:mm:ss" : "h:mm:ss aa"

	// If user's hub timezone is not set, display error messages in log and events log, and set timezone to GMT to avoid errors
    if (!(location.timeZone)) {
        correctedTimezone = TimeZone.getTimeZone("GMT")
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
    } 
    else {
        correctedTimezone = location.timeZone
    }

    if (dateformat == "US" || dateformat == "" || dateformat == null) {
        if (batteryReset)
            return new Date().format("MMM dd yyyy", correctedTimezone)
        else
            return new Date().format("EEE MMM dd yyyy ${timeString}", correctedTimezone)
    }
    else if (dateformat == "UK") {
        if (batteryReset)
            return new Date().format("dd MMM yyyy", correctedTimezone)
        else
            return new Date().format("EEE dd MMM yyyy ${timeString}", correctedTimezone)
        }
    else {
        if (batteryReset)
            return new Date().format("yyyy MMM dd", correctedTimezone)
        else
            return new Date().format("EEE yyyy MMM dd ${timeString}", correctedTimezone)
    }
}
