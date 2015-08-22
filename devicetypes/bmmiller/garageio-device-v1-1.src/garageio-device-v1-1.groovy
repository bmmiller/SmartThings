/**
 *  Garageio Device v1.1
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
 
preferences {
    input("email_address", "text", title: "Username", description: "Your Garageio username")
    input("password", "password", title: "Password", description: "Your Garageio password")
}
 
metadata {
	definition (name: "Garageio Device v1.1", namespace: "bmmiller", author: "Brandon Miller") {
	capability "Contact Sensor"
    capability "Sensor"
	capability "Polling"
    capability "Switch"
        
    attribute "status", "string"

	command "push"
	command "open"
	command "close"
	command "getAuthToken"
}
    
    tiles {
		standardTile("status", "device.status", width: 2, height: 2) {
			state("closed", label:'${name}', icon:"st.doors.garage.garage-closed", action: "push", backgroundColor:"#79b821", nextState:"opening")
			state("open", label:'${name}', icon:"st.doors.garage.garage-open", action: "push", backgroundColor:"#ffa81e", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffe71e")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffe71e")
		}
        
        standardTile("open", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"push", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"push", icon:"st.doors.garage.garage-closing"
		}
        
        standardTile("contact", "device.contact") {
			state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e")
			state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821")
		}
        
        standardTile("refresh", "device", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
		
        main(["status"])
		details(["status","open","close","refresh"])
	}
}

def poll() {
	log.debug "Executing poll()"   
    
    try {
        httpGet("https://garageio.com/api/controllers/SyncController.php?auth_token=" + data.auth.data[0].authentication_token + "&user_id=" + data.auth.userid) { response ->
            state.data = response.data
            log.trace "Polling Result: " + state.data.success       
        }
    }
    catch (e) {
    	getAuthToken()
    }
    
    if (!state.data.success)
    {
    	log.debug "Need to login, next scheduled poll will refresh, or manually choose refresh in app"
    	getAuthToken()       
    }
    
    if (state.data.data.devices[0].doors[0].state.toString() == "CLOSED")
    {
        sendEvent(name: 'status', value: 'closed')
        sendEvent(name: 'contact', value: 'closed')
    }
    else if (state.data.data.devices[0].doors[0].state.toString() == "OPEN")
    {
    	sendEvent(name: 'status', value: 'open')
        sendEvent(name: 'contact', value: 'open')
    }
}

def open() {
	push()
}

def close() {
	push()
}

def push() {
    def currentState = state.data.data.devices[0].doors[0].state
    def changeState = (currentState == "OPEN") ? "CLOSED" : "OPEN"
    log.debug "Current State: " + currentState
    log.debug "State Change: " + changeState + " Garage"
    log.debug "Door ID: " + state.data.data.devices[0].doors[0].id
    
    // Make sure we have credentials
    if (data.auth == null)
    {
        getAuthToken()
    }
    def params = [
        uri: "https://garageio.com/api/controllers/ToggleController.php",
        headers: [ "Content-Type": "application/x-www-form-urlencoded" ],
        body: [ 
            auth_token: data.auth.data[0].authentication_token, 
            user_id: data.auth.userid, 
            door_id: state.data.data.devices[0].doors[0].id, 
            door_state: changeState 
        ]
    ]

    def tryPost = true
    def responseCode

    while (tryPost) {
        httpPost(params) { response->
            log.debug response.data
            responseCode = response.data.code
        }

        if (responseCode == "401") {
            log.debug "Retrying login..."
            getAuthToken()           
            sleep(2) // Wait for 2 seconds to allow Garageio servers to recognize the new auth token they just created
        } else {
            tryPost = false
        }
    }

    if (responseCode == "200")
    {
        runIn(60, poll)
    }
}

def getAuthToken() {
    def params = [
        uri: "https://garageio.com/api/controllers/AuthController.php",
        body: [email_address: settings.email_address, password: settings.password]
    ]

    httpPost(params) {response ->
        data.auth = response.data
        log.debug data.auth
    }
}
