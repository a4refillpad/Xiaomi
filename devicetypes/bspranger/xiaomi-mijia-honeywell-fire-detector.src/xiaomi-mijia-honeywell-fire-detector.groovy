/**
 *  Xiaomi Aqara Zigbee Button
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
 *  Review in english photos dimensions etc... https://blog.tlpa.nl/2017/11/12/xiaomi-also-mijia-and-honeywell-smart-fire-detector/
 *  Device purchased here (€20.54)... https://www.gearbest.com/alarm-systems/pp_615081.html
 *  RaspBee packet sniffer... https://github.com/dresden-elektronik/deconz-rest-plugin/issues/152
 *  
 *        01 - endpoint id
 *        0104 - profile id
 *        0402 - device id
 *        01 - ignored
 *        03 - number of in clusters
 *        0000 0003 0012 0500 - inClusters
 *        01 - number of out clusters
 *        0019 - outClusters
 *        manufacturer "LUMI" - must match manufacturer field in fingerprint
 *        model "lumi.sensor_smoke" - must match model in fingerprint
 *        deviceJoinName: whatever you want it to show in the app as a Thing
 *
 */

metadata {
	definition (name: "Xiaomi Mijia Honeywell Fire Detector", namespace: "foz333", author: "foz333") {
        capability "Configuration"
        capability "Smoke Detector"
        capability "Sensor"
        capability "Battery"
        capability "Refresh"

        attribute "lastTested", "String"
        attribute "lastCheckin", "string"
        attribute "lastCheckinDate", "Date"
        attribute "batteryRuntime", "String"

        command "enrollResponse"
        command "resetBatteryRuntime"
 
	fingerprint endpointId: "01", profileID: "0104", deviceID: "0402", inClusters: "0000,0003,0012,0500", outClusters: "0019", manufacturer: "LUMI", model: "lumi.sensor_motion.aq2", deviceJoinName: "Xiaomi Honeywell Smoke Detector"
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
		multiAttributeTile(name:"smoke", type: "generic", width: 6, height: 4) {
			tileAttribute ("device.smoke", key: "PRIMARY_CONTROL") {
           			attributeState("clear", label:'CLEAR', icon:"st.alarm.smoke.clear", backgroundColor:"#ffffff")
            			attributeState("detected", label:'SMOKE', icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13")   
 			}
		}
        	valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            		state "default", label:'${currentValue}%', unit:"%", 
			backgroundColors:[
                		[value: 10, color: "#bc2323"],
                		[value: 26, color: "#f1d801"],
                		[value: 51, color: "#44b621"]
            		]
        	}
		standardTile("icon", "", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            		state "default", label:'Last Tested', icon:"st.alarm.smoke.test"
		}
		valueTile("lastTested", "device.lastTested", inactiveLabel: false, decoration: "flat", width: 4, height: 2) {
            		state "default", label:'${currentValue}'
		}
		valueTile("spacer", "spacer", decoration: "flat", inactiveLabel: false, width: 1, height: 1) {
	    		state "default", label:''
		}
		valueTile("lastcheckin", "device.lastCheckin", inactiveLabel: false, decoration:"flat", width: 4, height: 1) {
            		state "lastcheckin", label:'Last Event:\n ${currentValue}'
        	}
        	valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration:"flat", width: 4, height: 1) {
            		state "batteryRuntime", label:'Battery Changed: ${currentValue}'
        	}
		
		main (["smoke"])
		details(["smoke", "battery", "icon", "lastTested", "spacer", "lastcheckin", "spacer", "spacer", "batteryRuntime", "spacer"])
	}
}
  
