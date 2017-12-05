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
        
		fingerprint profileId: "0104", deviceId: "0104", inClusters: "0000", outClusters: "0000, 0004", manufacturer: "LUMI", model: "lumi.sensor_switch.aq2", deviceJoinName: "Original Xiaomi Aqara Button"
		fingerprint endpointId: "01", inClusters: "0000,FFFF,0006", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.sensor_switch.aq2", deviceJoinName: "Original Xiaomi Aqara Button"
	}
    
    simulator {
  		status "button 1 pressed": "on/off: 0"
      	status "button 1 released": "on/off: 1"
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
  log.debug "Parsing '${description}'"
//  send event for heartbeat    
  def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
  sendEvent(name: "lastCheckin", value: now)
  
  def results = []
  if (description?.startsWith('on/off: '))
		results = parseCustomMessage(description)
  if (description?.startsWith('catchall:')) 
		results = parseCatchAllMessage(description)
        
  return results;
}

def configure(){
    [
    "zdo bind 0x${device.deviceNetworkId} 1 2 0 {${device.zigbeeId}} {}", "delay 5000",
    "zcl global send-me-a-report 2 0 0x10 1 0 {01}", "delay 500",
    "send 0x${device.deviceNetworkId} 1 2"
    ]
}

def refresh(){
	"st rattr 0x${device.deviceNetworkId} 1 2 0"
    "st rattr 0x${device.deviceNetworkId} 1 0 0"
	log.debug "refreshing"
    sendEvent(name: 'numberOfButtons', value: 1)
    createEvent([name: 'batterylevel', value: '100', data:[buttonNumber: 1], displayed: false])
}

private Map parseCatchAllMessage(String description) {
	Map resultMap = [:]
	def cluster = zigbee.parse(description)
	log.debug cluster
	if (cluster) {
		switch(cluster.clusterId) {
			case 0x0000:
                resultMap = getBatteryResult(cluster.data.get(6))
			break

			case 0xFC02:
			log.debug 'ACCELERATION'
			break

			case 0x0402:
			log.debug 'TEMP'
				// temp is last 2 data values. reverse to swap endian
				String temp = cluster.data[-2..-1].reverse().collect { cluster.hex1(it) }.join()
				def value = getTemperature(temp)
				resultMap = getTemperatureResult(value)
				break
		}
	}

	return resultMap
}

private Map getBatteryResult(rawValue) {
	log.debug 'Battery'
	def linkText = getLinkText(device)
    
    def rawVolts = rawValue / 1000

	def maxBattery = state.maxBattery ?: 0
    def minBattery = state.minBattery ?: 0

	if (maxBattery == 0 || rawVolts > minBattery)
    	state.maxBattery = maxBattery = rawVolts
        
    if (minBattery == 0 || rawVolts < minBattery)
    	state.minBattery = minBattery = rawVolts
    
    def volts = (maxBattery + minBattery) / 2

	def minVolts = 2.0
    def maxVolts = 3.04
    def pct = (volts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))

	def result = [
		name: 'battery',
		value: roundedPct,
        unit: "%",
        isStateChange:true,
        descriptionText : "${device.displayName} raw battery is ${rawVolts}v, state: ${volts}v, ${minBattery}v - ${maxBattery}v"
	]
    
    log.debug result.descriptionText
    state.lastbatt = new Date().time
    return createEvent(result)
}

private Map parseCustomMessage(String description) {
	if (description?.startsWith('on/off: ')) {
    	if (description == 'on/off: 1') 		//button pressed
    		return createPressEvent(1)
    	else if (description == 'on/off: 0') 	//button released
    		return createButtonEvent(1)
	}
}

//this method determines if a press should count as a push or a hold and returns the relevant event type
private createButtonEvent(button) {
	def currentTime = now()
    log.debug "Button Pressed"
    return createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
}

private createPressEvent(button) {
	return createEvent([name: 'lastPress', value: now(), data:[buttonNumber: button], displayed: false])
}

//Need to reverse array of size 2
private byte[] reverseArray(byte[] array) {
    byte tmp;
    tmp = array[1];
    array[1] = array[0];
    array[0] = tmp;
    return array
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

def push() {
	log.debug "App Button Pressed"
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: false)
	sendEvent(name: "switch", value: "off", isStateChange: true, displayed: false)
	sendEvent(name: "momentary", value: "pushed", isStateChange: true)
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName button 1 was pushed", isStateChange: true)
}

def on() {
	push()
}

def off() {
	push()
}