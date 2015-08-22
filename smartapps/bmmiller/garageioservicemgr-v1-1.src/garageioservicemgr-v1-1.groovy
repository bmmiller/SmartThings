/**
 *  GarageioServiceMgr v1.1
 *
 *  Copyright 2015 Brandon Miller
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
 
import groovy.json.JsonSlurper
 
definition(
    name: "GarageioServiceMgr v1.1",
    namespace: "bmmiller",
    author: "Brandon Miller",
    description: "Initializes and polls Garageio device",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/bmmiller/SmartThings/master/devicetype.garageio/garageio.png",
    iconX2Url: "https://raw.githubusercontent.com/bmmiller/SmartThings/master/devicetype.garageio/garageio.png",
    iconX3Url: "https://raw.githubusercontent.com/bmmiller/SmartThings/master/devicetype.garageio/garageio.png")


preferences {
	page(name: "about", title: "About", nextPage: "authInfo")
	page(name: "authInfo", title: "Garageio", nextPage:"deviceList")
	page(name: "deviceList", title: "Garageio Controlled Doors", content:"GarageioDeviceList")         
    page(name: "otherSettings", title: "Other Settings", content:"otherSettings", install:true)
}

def about() {
 	dynamicPage(name: "about", install: false, uninstall: true) {
 		section("About") {	
			paragraph "GarageioServiceMgr, the smartapp that initializes your Garageio device and polls it on a regular basis"
			paragraph "Version 1.1\n\n" +
				"If you like this app, please support the developer via PayPal:\n\bmmiller@gmail.com\n\n" +
				"Copyright©2015 Brandon Miller"
			href url: "http://github.com/bmmiller", style: "embedded", required: false, title: "More information..."
		} 
	}        
}

def authInfo() {
	dynamicPage(name: "authInfo") {
    	section("Provide login information") {
    		input("email_address", "text", title: "Username", description: "Your Garageio username")
    		input("password", "password", title: "Password", description: "Your Garageio password")
		}
    }
}

def GarageioDeviceList() {
	log.trace "GarageioDeviceList()"
    
    def garageioDoors = getGarageioDoors()
    
    log.trace "device list: $garageioDoors"
    
    def p = dynamicPage(name: "deviceList", title: "Select Your Doors(s)",nextPage:"otherSettings") {
		section(""){
			paragraph "Tap below to see the list of Garageio Controlled Doors available on your Garageio Device and select the ones you want to connect to SmartThings."
			input(name: "GarageioDoors", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:garageioDoors])
		}
	}

	log.debug "list p: $p"
	return p    	     
}

def getGarageioDoors() {
	def GARAGEIO_SUCCESS=200
    
	log.debug "Getting Garageio doors list"    
    def auth = getAuthToken()
    def token = auth.data[0].authentication_token
    def userid = auth.userid
    def doors = [:]
    
    try 
    {
    	httpGet("https://garageio.com/api/controllers/SyncController.php?auth_token=" + token + "&user_id=" + userid)
        	{ response ->            
            	if (response.status == GARAGEIO_SUCCESS)
                {                                            
                    response.data.data.devices[0].doors.each {
						def doorId = it.id
                        def state = it.state
                        def name = it.name
						def dni = [ app.id, name, state, doorId, ].join('.')
						doors[dni] = name
					}                    
                }
    		}            
    }
    catch (e) {
		state?.msg= "exception $e while getting list of Doors" 
		log.error state.msg        
    }   
    
    //log.debug "doors: $doors"

	return doors
}

def otherSettings() {
	dynamicPage(name: "otherSettings", title: "Other Settings", install: true, uninstall: false) {
    	section("Polling at which interval in minutes (range=[5..59],default=30 min.)?") {
			input "givenInterval", "number", title:"Interval", required: false
		}
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input "phoneNumber", "phone", title: "Send a text message?", required: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
	}
}

def getAuthToken() {
    def params = [
        uri: "https://garageio.com/api/controllers/AuthController.php",
        body: [email_address: email_address, password: password]
    ]
	
    httpPost(params) {response ->       
        return response.data
    }
}

def poll() {

}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()    
	initialize()
}

private def delete_child_devices() {
	def delete
    
	// Delete any that are no longer in settings
	if (!GarageioDoors) {
		log.debug "delete_child_devices> deleting all Garageio Doors"
		delete = getAllChildDevices()
	} else {
		delete = getChildDevices().findAll { !GarageioDoors.contains(it.deviceNetworkId) }
		log.debug "delete_child_devices> deleting ${delete.size()} Garageio Doors"	
	}

	try { 
		delete.each { deleteChildDevice(it.deviceNetworkId) }
	} catch (e) {
		log.debug "delete_child_devices> exception $e while deleting ${delete.size()} Garageio Doors"
	}	
}

private def create_child_devices() {
	def devices = GarageioDoors.collect { dni ->
    
        def d = getChildDevice(dni)
        log.debug "create_child_devices> looping thru Garageio Doors, found id $dni"

        if(!d) {
            def garageio_info  = dni.tokenize('.')
            log.debug "garageio_info: ${garageio_info}"
            def doorId = garageio_info.last()
            def labelName = 'Garageio ' + "${doorId}"
            log.debug "create_child_devices> about to create child device with id $dni, doorId = $doorId"
            d = addChildDevice(getChildNamespace(), getChildName(), dni, null,
                               [label: "${labelName}"]) 
            //d.initialSetup( getSmartThingsClientId(), atomicState, doorId ) 	// initial setup of the Child Device
            log.debug "create_child_devices> created ${d.displayName} with id $dni"

        } else {
            log.debug "create_child_devices>found ${d.displayName} with id $dni already exists"
        }
	}

	log.debug "create_child_devices> created ${devices.size()} Garageio door"
}

def initialize() {
	log.debug "initialize"
	state?.exceptionCount=0
	state?.msg=null    
	delete_child_devices()	
	create_child_devices() 
    
    Integer delay = givenInterval ?: 30 // By default, do it every 30 min.
	if ((delay < 5) || (delay>59)) {
		state?.msg= "GarageioServiceMgr> scheduling interval not in range (${delay} min), exiting..."
		log.debug state.msg
		runIn(30, "sendMsgWithDelay")
 		return
	}
	schedule("0 0/${delay} * * * ?", takeAction)	
}

def takeAction() {
	log.trace "takeAction> begin"
	def devices = GarageioDoors.collect { dni ->
    
		def d = getChildDevice(dni)
		log.debug "takeAction> looping thru Garageio Doors, found id $dni, about to poll"
		try {
			d.poll()               
		} catch (e) {       
			log.error "GarageioServiceMgr> exception $e while trying to poll the device $d, exceptionCount= ${state?.exceptionCount}" 
		}    
	}
    log.trace "takeAction> end"
}

def getChildName() { "Garageio Device v1.1" }

def getChildNamespace() { "bmmiller" }

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}