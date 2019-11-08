/* 
 *  Xmas Tree Controller v1.5.0 - 2019-10-07
 *
 * 	Changelog
 *		v1.5.0	- With the new Bond App Removing IFTTT support and adding Local API support, this is a refactor to the Local API instead of utilizing IFTTT.  Requires the new 
 *			    - version of the Bond App, new firmware (v2.9.4 as of today), and a NAT entry in your firewall to the local Bond IP.  Something like port 10000 being forwarded
 *			    - to port 80 of the bond and then the URI in the preferences being the WAN IP of your home/firewall/ISP.
 *		v1.0.0	- Initial push of Xmas Tree Controller Device.  This uses the Bond RF Controller (https://bondhome.io) to attach the Balsam Xmas Tree (https://www.balsamhill.com/) 
 *              - RF controller as a type of fan with 3 "speeds".  1 being the clear lights setting, 2 being the multi lights setting, and 3 being all lights.  It relies
 *			    - on IFTTT integration and enabling of webhooks.  Input your key into the the preferences, and you'll have to create the rules on IFTTT to trigger on
 *			    - the following events: xmas_tree_clear, xmas_tree_multi, xmas_tree_all, and xmas_tree_off.  Within IFTTT they will trigger the percentage from
 *			    - within the Bond "that" integration, 20% = 1 Speed in Bond = xmas_tree_clear, 50% = 2 Speed in Bond = xmas_tree_multi, 80% = 3 Speed in Bond = xmas_tree_all and finally,
 *			    - Off = Off in Bond = xmas_tree_off.  I found it was best to use webCoRE (https://dashboard.webcore.co) to schedule because of the custom commands.
 *			    - If you use would prefer to not use webCoRE and just used standard scheduling.  Whatever your set for your default, will be what happens when you turn the switch on.
 * 
 *  Copyright 2019 Brandon Miller
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
 */
metadata {

    definition (name: "Xmas Tree Controller", namespace: "bmmiller", author: "Brandon Miller", runLocally: false, mnmn: "SmartThings", vid: "generic-xmas-controller") {
		capability "Switch"
		capability "Relay Switch"
		
        command "getDefault"
		command "clearLights"
		command "multiLights"
		command "allLights"
    }

    tiles {
		standardTile("switch", "device.switch", width: 3, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "getDefault", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
        }
		
        standardTile("switchClear", "device.switchClear", decoration: "flat") {
            state "off", label: 'Clear Lights: ${currentValue}', icon: "st.switches.light.off", action: "clearLights", backgroundColor: "#ffffff"
            state "on", label: 'Clear Lights: ${currentValue}', icon: "st.switches.light.on", action: "off", backgroundColor: "#ffffff"
        }
		
        standardTile("switchMulti", "device.switchMulti", decoration: "flat") {
            state "off", label: 'Multi Lights: ${currentValue}', icon: "st.switches.light.off", action: "multiLights", backgroundColor: "#ffffff"
            state "on", label: 'Multi Lights: ${currentValue}', icon: "st.switches.light.on", action: "off", backgroundColor: "#ffffff"
        }
		
		standardTile("switchAll", "device.switchAll", decoration: "flat") {
            state "off", label: 'All Lights: ${currentValue}', icon: "st.switches.light.off", action: "allLights", backgroundColor: "#ffffff"
            state "on", label: 'All Lights: ${currentValue}', icon: "st.switches.light.on", action: "off", backgroundColor: "#ffffff"
        }
	      
        main "switch"
        details(["switch","switchClear","switchMulti","switchAll"])
    }
    
    preferences {
    	input name: "uri", type: "text", title: "Endpoint URI", description: "IP/Hostname:PORT", required: true, displayDuringSetup: true
    	input name: "bondToken", type: "text", title: "BOND-Token", description: "Your BOND Token", required: true, displayDuringSetup: true
        input name: "deviceId", type: "text", title: "Device ID", description: "Device ID to Control", required: true, displayDuringSetup: true
        input name: "defaultLights", type: "enum", title: "Default Lights", options: ["Clear Lights", "Multi Lights", "All Lights"], description: "Choose a default lighting setup", required: true
        input name: "offCommandId", type: "text", title: "Off Command Id", description: "Enter your turn off command id", required: true, displayDuringSetup: true
        input name: "clearLightsCommandId", type: "text", title: "Clear Lights Command Id", description: "Enter your clear lights command id", required: true, displayDuringSetup: true
        input name: "multiLightsCommandId", type: "text", title: "Multi Lights Command Id", description: "Enter your multi lights command id", required: true, displayDuringSetup: true
        input name: "allLightsCommandId", type: "text", title: "All Lights Command Id", description: "Enter your all lights command id", required: true, displayDuringSetup: true
	}
}

def installed() {
    log.trace "Executing 'installed'"
	off()
    initialize()
}

def updated() {
    log.trace "Executing 'updated'"
    initialize()
}

private initialize() {
    log.trace "Executing 'initialize'"    
}

def getDefault() {
	log.debug "getDefault: ${defaultLights}"
    
    sendEvent(name: "switch", value: "on")
    
	switch (defaultLights) {
    	case "Clear Lights":
        	clearLights()
            break
        case "Multi Lights":
        	multiLights()
            break
        case "All Lights":
        	allLights()
            break
    }
}

def off() {
    log.debug "off()"
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "switchClear", value: "off")
    sendEvent(name: "switchMulti", value: "off")
    sendEvent(name: "switchAll", value: "off")
	sendGetRequest("${offCommandId}")
}

def clearLights() {
    sendEvent(name: "switchClear", value: "on")
	sendGetRequest("${clearLightsCommandId}")
	syncButtons("clearLights")
}

def multiLights() {
    sendEvent(name: "switchMulti", value: "on")
	sendGetRequest("${multiLightsCommandId}")
	syncButtons("multiLights")
}

def allLights() {
	sendEvent(name: "switchAll", value: "on")	
	sendGetRequest("${allLightsCommandId}")
	syncButtons("allLights")
}

private syncButtons(method){
	sendEvent(name: "switch", value: "on")
    
    switch(method) {
    	case "clearLights":
        	sendEvent(name: "switchMulti", value: "off")
            sendEvent(name: "switchAll", value: "off")
        	break
        case "multiLights":
        	sendEvent(name: "switchClear", value: "off")
            sendEvent(name: "switchAll", value: "off")
        	break
            
        case "allLights":
        	sendEvent(name: "switchClear", value: "off")
            sendEvent(name: "switchMulti", value: "off")
        	break
    }
}

private sendGetRequest(event) {
	log.debug "sendPutRequest: http://${uri}/v2/devices/${deviceId}/commands/${event}/tx"
    def params = [
    	uri: "http://${uri}",
        path: "/v2/devices/${deviceId}/commands/${event}/tx",
        headers: ["BOND-Token":"${bondToken}"],
        body: "{}"
    ]

    try
	{
		httpPut(params) { }
	}
	catch (e) {
		log.error "Something went wrong: $e"
	}
}
