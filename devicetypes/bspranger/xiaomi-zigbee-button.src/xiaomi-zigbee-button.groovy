/**
 *  Xiaomi Zigbee Button
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
 *  added 100% battery max
 *  fixed battery parsing problem
 *  added lastcheckin attribute and tile
 *  added a means to also push button in as tile on smartthings app
 *  fixed ios tile label problem and battery bug 
 *  sulee: change battery calculation
 *  sulee: changed to work as a push button
 *  sulee: added endpoint for Smartthings to detect properly
 *  sulee: cleaned everything up
 *  bspranger: renamed to bspranger to remove confusion of a4refillpad
 *
 *  Fingerprint Endpoint data:
 *  zbjoin: {"dni":"xxxx","d":"xxxxxxxxxxx","capabilities":"80","endpoints":[{"simple":"01 0104 5F01 01 03 0000 FFFF 0006 03 0000 0004 FFFF","application":"03","manufacturer":"LUMI","model":"lumi.sensor_switch.aq2"}],"parent":"0000","joinType":1}
 *     endpoints data
 *        01 - endpoint id
 *        0104 - profile id
 *        5F01 - device id
 *        01 - ignored
 *        03 - number of in clusters
 *        0000 ffff 0006 - inClusters
 *        03 - number of out clusters
 *        0000 0004 ffff - outClusters
 *        manufacturer "LUMI" - must match manufacturer field in fingerprint
 *        model "lumi.sensor_switch.aq2" - must match model in fingerprint
 *        deviceJoinName: whatever you want it to show in the app as a Thing
 *
 */
metadata {
    definition (name: "xiaomi-zigbee-button", namespace: "bspranger", author: "bspranger") {
        capability "Battery"
        capability "Button"
		capability "Configuration"
		capability "Sensor"
        capability "Refresh"
        
        attribute "lastPress", "string"
        attribute "batterylevel", "string"
        attribute "lastCheckin", "string"
        attribute "lastCheckinDate", "Date"
        
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_switch", deviceJoinName: "Original Xiaomi Button"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_switch.aq2", deviceJoinName: "Xiaomi Aqara Button"
    }
    
    simulator {
          status "button 1 pressed": "on/off: 0"
      	status "button 1 released": "on/off: 1"
    }
    
    preferences{
    	input ("holdTime", "number", title: "Minimum time in seconds for a press to count as \"held\"",
        		defaultValue: 4, displayDuringSetup: false)
    }
    
    tiles(scale: 2) {

        multiAttributeTile(name:"button", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.button", key: "PRIMARY_CONTROL") {
                   attributeState("pushed", label:'${name}', backgroundColor:"#53a7c0")
                attributeState("released", label:'${name}', backgroundColor:"#ffffff")
             }
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
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
        valueTile("lastopened", "device.lastOpened", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Open:\n${currentValue}'
        }
        standardTile("refresh", "command.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
        }
        main (["button"])
        details(["button","battery","lastcheckin","lastopened","refresh"])
   }
}

def parse(String description) {
    def linkText = getLinkText(device)
    log.debug "${linkText}: Parsing '${description}'"

    //  send event for heartbeat    
    def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    def nowDate = new Date(now).getTime()
    sendEvent(name: "lastCheckin", value: now)
    sendEvent(name: "lastCheckinDate", value: nowDate) 

    Map map = [:]

    if (description?.startsWith('on/off: ')) 
    {
        map = parseCustomMessage(description) 
        sendEvent(name: "lastOpened", value: now)
        sendEvent(name: "lastOpenedDate", value: nowDate) 
    }
    else if (description?.startsWith('catchall:')) 
    {
        map = parseCatchAllMessage(description)
    }
    else if (description?.startsWith("read attr - raw: "))
    {
        map = parseReadAttrMessage(description)  
    }
    log.debug "${linkText}: Parse returned $map"
    def results = map ? createEvent(map) : null

    return results;
}

def configure(){
    def linkText = getLinkText(device)
    log.debug "${linkText}: configuring"
    return zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 0x01)
}

def refresh(){
    def linkText = getLinkText(device)
    log.debug "${linkText}: refreshing"
    return zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 0x01)
}

private Map parseReadAttrMessage(String description) {
    def linkText = getLinkText(device)
    def buttonRaw = (description - "read attr - raw:")
    Map resultMap = [:]

    def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
    def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
    def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
    def model = value.split("01FF")[0]
    def data = value.split("01FF")[1]
    log.debug "cluster: ${cluster}, attrId: ${attrId}, value: ${value}, model:${model}, data:${data}"
    
    if (data[4..7] == "0121") {
    	def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))
        resultMap = getBatteryResult(BatteryVoltage)
        log.debug "${linkText}: Parse returned $resultMap"
        createEvent(resultMap)
    }

    if (cluster == "0000" && attrId == "0005")  {
        resultMap.name = 'Model'
        resultMap.value = ""
        resultMap.descriptionText = "device model"
        // Parsing the model
        for (int i = 0; i < model.length(); i+=2) 
        {
            def str = model.substring(i, i+2);
            def NextChar = (char)Integer.parseInt(str, 16);
            resultMap.value = resultMap.value + NextChar
        }
        return resultMap
    }
    
    return [:]    
}

private Map parseCatchAllMessage(String description) {
    def linkText = getLinkText(device)
    def MsgLength
    def i
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    log.debug cluster
    if (cluster) {
        switch(cluster.clusterId) {
            case 0x0000:
            	MsgLength = cluster.data.size();
                for (i = 0; i < (MsgLength-3); i++)
                {
                    if ((cluster.data.get(i) == 0x01) && (cluster.data.get(i+1) == 0x21))  // check the data ID and data type
                    {
                        // next two bytes are the battery voltage.
                        resultMap = getBatteryResult((cluster.data.get(i+3)<<8) + cluster.data.get(i+2))
                    }
                }
            break
        }
    }
    return resultMap
}

private Map getBatteryResult(rawValue) {
    def linkText = getLinkText(device)
    def rawVolts = rawValue / 1000

    def maxBattery = state.maxBattery ?: 0
    def minBattery = state.minBattery ?: 0

    if (maxBattery == 0 || rawVolts > minBattery)
        state.maxBattery = maxBattery = rawVolts
        
    if (minBattery == 0 || rawVolts < minBattery)
        state.minBattery = minBattery = rawVolts
    
    def volts = (maxBattery + minBattery) / 2

    def minVolts = 2.7
    def maxVolts = 3.0
    def pct = (volts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))

    def result = [
        name: 'battery',
        value: roundedPct,
        unit: "%",
        isStateChange:true,
        descriptionText : "${device.displayName} raw battery is ${rawVolts}v, state: ${volts}v, ${minBattery}v - ${maxBattery}v"
    ]
    
    log.debug "${linkText}: ${result}"
    state.lastbatt = new Date().time
    return createEvent(result)
}

private Map parseCustomMessage(String description) {
    def linkText = getLinkText(device)
    def result = [:]
    if (description?.startsWith('on/off: ')) {
        if (description == 'on/off: 0'){
            result = getContactResult("pushed")
            log.debug "${linkText}: Parse returned $result"
            createEvent(result)
            result = getContactResult("released")
        }
        return result
    }
}

private Map getContactResult(value) {
    def linkText = getLinkText(device)
    def descriptionText = "${linkText} was ${value == 'pushed' ? 'pushed' : 'released'}"
    return [
        name: 'button',
        value: value,
        descriptionText: descriptionText
    ]
}