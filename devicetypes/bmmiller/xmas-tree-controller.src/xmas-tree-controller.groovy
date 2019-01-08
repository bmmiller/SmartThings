/* 
 *  Xmas Tree Controller v1.0.0 - 2019-01-08
 *
 * 		Changelog
 *			v1.0.0	- Initial push of Xmas Tree Controller Device.  This uses the Bond RF Controller (https://bondhome.io) to attach the Balsam Xmas Tree (https://www.balsamhill.com/) 
 *                  - RF controller as a type of fan with 3 "speeds".  1 being the clear lights setting, 2 being the multi lights setting, and 3 being all lights.  It relies
 *					- on IFTTT integration and enabling of webhooks.  Input your key into the the preferences, and you'll have to create the rules on IFTTT to trigger on
 *					- the following events: xmas_tree_clear, xmas_tree_multi, xmas_tree_all, and xmas_tree_off.  Within IFTTT they will trigger the percentage from
 *					- within the Bond "that" integration, 20% = 1 Speed in Bond = xmas_tree_clear, 50% = 2 Speed in Bond = xmas_tree_multi, 80% = 3 Speed in Bond = xmas_tree_all and finally,
 *					- Off = Off in Bond = xmas_tree_off.  I found it was best to use webCoRE (https://dashboard.webcore.co) to schedule because of the custom commands.
 *					- If you use would prefer to not use webCoRE and just used standard scheduling.  Whatever your set for your default, will be what happens when you turn the switch on.
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
    	input name: "webhookKey", type: "text", title: "Key", description: "Your IFTTT Webhook Key", required: true, displayDuringSetup: true
        input name: "defaultLights", type: "enum", title: "Default Lights", options: ["Clear Lights", "Multi Lights", "All Lights"], description: "Choose a default lighting setup", required: true
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
	sendGetRequest("xmas_tree_off")
}

// Will send 20% percentage command to Bond
def clearLights() {
    sendEvent(name: "switchClear", value: "on")
	sendGetRequest("xmas_tree_clear")
	syncButtons("clearLights")
}

// Will send 50% percentage command to Bond
def multiLights() {
    sendEvent(name: "switchMulti", value: "on")
	sendGetRequest("xmas_tree_multi")
	syncButtons("multiLights")
}

// Will send 80% percentage command to Bond
def allLights() {
	sendEvent(name: "switchAll", value: "on")	
	sendGetRequest("xmas_tree_all")
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
	log.debug "sendGetRequest: https://maker.ifttt.com/trigger/${event}/with/key/${webhookKey}"
    try
	{
		httpGet("https://maker.ifttt.com/trigger/${event}/with/key/${webhookKey}") { resp ->
			resp.headers.each {
			   log.debug "${it.name} : ${it.value}"
			}
			log.debug "response contentType: ${resp.contentType}"
			log.debug "response data: ${resp.data}"
		}
	}
	catch (e) {
		log.error "Something went wrong: $e"
	}
}