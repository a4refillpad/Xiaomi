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
 *
 *  Fingerprint Endpoint data:
 *  zbjoin: {"dni":"A223","d":"00158D0001B767E0","capabilities":"80","endpoints":[{"simple":"01 0104 5F01 01 03 0000 FFFF 0006 03 0000 0004 FFFF","application":"03","manufacturer":"LUMI","model":"lumi.sensor_switch.aq2"}],"parent":"0000","joinType":1}
 *     endpoints data, data size: short
 *        01 - size of device/profile id in short
 *        0104 - device/profile id
 *        5F01 01 - Unknown
 *        03 - size of inClusters in short
 *        0000 ffff 0006 - inClusters
 *        03 - size of outClusters in short
 *        0000 0004 ffff 0 outClusters
 *        manufacturer "LUMI" - must match manufacturer field in fingerprint
 *        model "lumi.sensor_switch.aq2" - must match model in fingerprint
 *        deviceJoinName: whatever you want it to show in the app as a Thing
 *
 */
metadata {
	definition (name: "Original Xiaomi Aqara Button", namespace: "a4refillpad", author: "a4refillpad") {	
    	capability "Battery"
		capability "Button"
		capability "Actuator"
		capability "Switch"
		capability "Momentary"
		capability "Sensor"
		capability "Refresh"
        
		attribute "lastPress", "string"
		attribute "batterylevel", "string"
		attribute "lastCheckin", "string"
        attribute "lastCheckinDate", "Date"
        
    	fingerprint endpointId: "01", profileId: "0104", deviceId: "5F01", inClusters: "0000,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_switch.aq2", deviceJoinName: "Original Xiaomi Aqara Button"
	}
    
    simulator {
  		status "button 1 pressed": "on/off: 0"
    }
    
	tiles(scale: 2) {

		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
           		attributeState("on", label:' push', action: "momentary.push", backgroundColor:"#53a7c0")
                attributeState("off", label:' push', action: "momentary.push", backgroundColor:"#ffffff", nextState: "on")
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
                    [value: 51, color: "#44b621"] ]
        }

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
		main "switch"
		details(["switch", "battery", "refresh", "configure"])
	}
}

def parse(String description) {
	def result = zigbee.getEvent(description)

	//  send event for heartbeat    
	def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
	def nowDate = new Date(now).getTime()
	sendEvent(name: "lastCheckin", value: now)
	sendEvent(name: "lastCheckinDate", value: nowDate)

    if (description?.startsWith('catchall:')) {
		return parseCatchAllMessage(description)
	} else if (result) {
        push()
    }
}

def configure(){
	def linkText = getLinkText(device)
    log.debug "${linkText}: configuring"
    return zigbee.readAttribute(0x0001, 0x0021) + zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 0x01)
}

def refresh(){
	def linkText = getLinkText(device)
    log.debug "${linkText}: refreshing"
    return zigbee.readAttribute(0x0001, 0x0021) + zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 0x01)
}

private Map parseCatchAllMessage(String description) {
	def linkText = getLinkText(device)
	Map resultMap = [:]
	def cluster = zigbee.parse(description)
	log.debug "${linkText}: Parsing CatchAll: '${cluster}'"
	if (cluster) {
		switch(cluster.clusterId) {
			case 0x0000:
            	if ((cluster.data.get(4) == 1) && (cluster.data.get(5) == 0x21)) // Check CMD and Data Type
            		resultMap = getBatteryResult((cluster.data.get(7)<<8) + cluster.data.get(6))
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

def push() {
	def linkText = getLinkText(device)
	log.debug "${linkText}: Button Pressed"
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: false)
	sendEvent(name: "switch", value: "off", isStateChange: true, displayed: false)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Button pushed", isStateChange: true)
}
