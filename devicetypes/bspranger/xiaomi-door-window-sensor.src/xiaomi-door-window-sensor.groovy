/**
 *  Xiaomi Door/Window Sensor
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
 * Based on original DH by Eric Maycock 2015 and Rave from Lazcad
 *  change log:
 *    added DH Colours
 *  added 100% battery max
 *  fixed battery parsing problem
 *  added lastcheckin attribute and tile
 *  added extra tile to show when last opened
 *  colours to confirm to new smartthings standards
 *  added ability to force override current state to Open or Closed.
 *  added experimental health check as worked out by rolled54.Why
 *  Rinkelk - added date-attribute support for Webcore
 *  Rinkelk - Changed battery icon according to Mobile785
 *  bspranger - renamed to bspranger to remove confusion of a4refillpad
 *
 */
preferences {
	input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", required: false, options:["US","UK","Other"]
	input description: "Only change the settings below if you know what you're doing", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
	input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
	input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
} 

metadata {
   definition (name: "Xiaomi Door/Window Sensor", namespace: "bspranger", author: "bspranger") {
   capability "Configuration"
   capability "Sensor"
   capability "Contact Sensor"
   capability "Refresh"
   capability "Battery"
   capability "Health Check"
   
   attribute "lastCheckin", "String"
   attribute "lastOpened", "String"
   attribute "lastOpenedDate", "Date" 
   attribute "lastCheckinDate", "Date"
   attribute "batteryRuntime", "String"

   fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000, 0003, FFFF, 0019", outClusters: "0000, 0004, 0003, 0006, 0008, 0005 0019", manufacturer: "LUMI", model: "lumi.sensor_magnet", deviceJoinName: "Xiaomi Door Sensor"
   
   command "resetBatteryRuntime"
   command "resetClosed"
   command "resetOpen"
   }
    
   simulator {
      status "closed": "on/off: 0"
      status "open": "on/off: 1"
   }
    
   tiles(scale: 2) {
        multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
            tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
                attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#e86d13"
                attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#00a0dc"
            }
            tileAttribute("device.lastOpened", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'Last Opened: ${currentValue}')
            }
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:'${currentValue}%', unit:"",
            backgroundColors: [
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"] 
            ]
        }
        valueTile("lastcheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Checkin:\n${currentValue}'
        }
        standardTile("resetClosed", "device.resetClosed", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"resetClosed", label: "Override Close", icon:"st.contact.contact.closed"
        }
        standardTile("resetOpen", "device.resetOpen", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"resetOpen", label: "Override Open", icon:"st.contact.contact.open"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "batteryRuntime", label:'Battery Changed (tap to reset):\n ${currentValue}', unit:"", action:"resetBatteryRuntime"
        }
        main (["contact"])
        details(["contact","battery","resetClosed","resetOpen","lastcheckin","batteryRuntime","refresh"])
   }
}

def parse(String description) {
    log.debug "${device.displayName}: Parsing '${description}'"

    //  send event for heartbeat    
    def now = formatDate()
    def nowDate = new Date(now).getTime()
    sendEvent(name: "lastCheckin", value: now)
    sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false) 

    Map map = [:]

    if (description?.startsWith('on/off: ')) 
    {
        map = parseCustomMessage(description) 
        sendEvent(name: "lastOpened", value: now, displayed: false)
        sendEvent(name: "lastOpenedDate", value: nowDate, displayed: false) 
    }
    else if (description?.startsWith('catchall:')) 
    {
        map = parseCatchAllMessage(description)
    }
    else if (description?.startsWith("read attr - raw: "))
    {
        map = parseReadAttrMessage(description)  
    }
    log.debug "${device.displayName}: Parse returned $map"
    def results = map ? createEvent(map) : null

    return results;
}

private Map parseReadAttrMessage(String description) {
    def result = [
        name: 'Model',
        value: ''
    ]
    def cluster
    def attrId
    def value
        
    cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
    attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
    value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
    //log.debug "cluster: ${cluster}, attrId: ${attrId}, value: ${value}"
    
    if (cluster == "0000" && attrId == "0005") 
    {
        // Parsing the model
        for (int i = 0; i < value.length(); i+=2) 
        {
            def str = value.substring(i, i+2);
            def NextChar = (char)Integer.parseInt(str, 16);
            result.value = result.value + NextChar
        }
    }
    //log.debug "Result: ${result}"
    return result
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
        isStateChange:true,
        descriptionText : "${device.displayName} raw battery is ${rawVolts}v"
    ]
    
    log.debug "${device.displayName}: ${result}"
    if (state.battery != result.value)
    {
    	state.battery = result.value
        resetBatteryRuntime()
    }
    return result
}

private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    def i
    log.debug "${device.displayName}: Parsing CatchAll: ${cluster}"
    if (cluster) {
        switch(cluster.clusterId) {
            case 0x0000:
                def MsgLength = cluster.data.size();
                for (i = 0; i < (MsgLength-3); i++)
                {
                    // Original Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
                    if ((cluster.data.get(0) == 0x02) && (cluster.data.get(1) == 0xFF))
                    {
                        if (cluster.data.get(i) == 0x21) // check the data ID and data type
                        {
                            // next two bytes are the battery voltage.
                            resultMap = getBatteryResult((cluster.data.get(i+2)<<8) + cluster.data.get(i+1))
                            return resultMap
                        }
                    }
                }
                break
        }
    }

    return resultMap
}




private Map parseCustomMessage(String description) {
    def result
    if (description?.startsWith('on/off: ')) {
        if (description == 'on/off: 0')         //contact closed
            result = getContactResult("closed")
        else if (description == 'on/off: 1')     //contact opened
            result = getContactResult("open")
        return result
    }
}

private Map getContactResult(value) {
    def descriptionText = "${device.displayName} was ${value == 'open' ? 'opened' : 'closed'}"
    return [
        name: 'contact',
        value: value,
        descriptionText: descriptionText
    ]
}

def resetClosed() {
    sendEvent(name:"contact", value:"closed")
} 

def resetOpen() {
    sendEvent(name:"contact", value:"open")
}

def resetBatteryRuntime() {
    def now = formatDate(true)   
    sendEvent(name: "batteryRuntime", value: now)
}

def refresh(){
    log.debug "${device.displayName}: refreshing"
    checkIntervalEvent("refresh");
    return zigbee.readAttribute(0x0006, 0x0000) +
    // Read cluster 0x0006 (on/off status)
    zigbee.configureReporting(0x0006, 0x0000, 0x10, 1, 7200, null)
    // cluster 0x0006, attr 0x0000, datatype 0x10 (boolean), min 1 sec, max 7200 sec, reportableChange = null (because boolean)
}

def configure() {
    log.debug "${device.displayName}: configuring"
    state.battery = 0
    checkIntervalEvent("configure");
    return zigbee.configureReporting(0x0006, 0x0000, 0x10, 1, 7200, null) +
    // cluster 0x0006, attr 0x0000, datatype 0x10 (boolean), min 1 sec, max 7200 sec, reportableChange = null (because boolean)
    zigbee.readAttribute(0x0006, 0x0000) 
    // Read cluster 0x0006 (on/off status)
}

def installed() {
    state.battery = 0
    checkIntervalEvent("installed");
}

def updated() {
    checkIntervalEvent("updated");
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when ${text}()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
    if (dateformat == "US" || dateformat == "" || dateformat == null) {
        if (batteryReset)
            return new Date().format("MMM dd yyyy", location.timeZone)
        else
            return new Date().format("EEE MMM dd yyyy h:mm:ss a", location.timeZone)
    }
    else if (dateformat == "UK") {
        if (batteryReset)
            return new Date().format("dd MMM yyyy", location.timeZone)
        else
            return new Date().format("EEE dd MMM yyyy h:mm:ss a", location.timeZone)
        }
    else {
        if (batteryReset)
            return new Date().format("yyyy MMM dd", location.timeZone)
        else
            return new Date().format("EEE yyyy MMM dd h:mm:ss a", location.timeZone)
    }
}
