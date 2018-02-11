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
 
		    fingerprint endpointId: "01", profileID: "0104", deviceID: "0402", inClusters: "0000,0003,0012,0500", outClusters: "0019", deviceJoinName: "Xiaomi Honeywell Smoke Detector"
        
}
  
