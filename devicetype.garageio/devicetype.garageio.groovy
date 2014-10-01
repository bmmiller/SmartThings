/**
 *  Garageio
 *
 *  Copyright 2014 Brandon Miller
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
    input("email_address", "text", title: "Username", description: "Your Garageio username (usually an email address)")
    input("password", "password", title: "Password", description: "Your Garageio password")
    input("door_id", "text", title: "Door ID", description: "Your Garageio Door ID")
}
 
metadata {
	definition (name: "Garageio", namespace: "bmmiller", author: "Brandon Miller") {
		capability "Contact Sensor"
        capability "Sensor"
		capability "Polling"
        
        attribute "status", "string"
        
        command "actuate"
	}

	simulator {
		// TODO: define status and reply messages here
	}
    
    tiles {
		standardTile("status", "device.status", width: 2, height: 2) {
			state("closed", label:'${name}', icon:"st.doors.garage.garage-closed", action: "actuate", backgroundColor:"#79b821", nextState:"opening")
			state("open", label:'${name}', icon:"st.doors.garage.garage-open", action: "actuate", backgroundColor:"#ffa81e", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffe71e")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffe71e")
		}
        
        standardTile("contact", "device.contact") {
			state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e")
			state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821")
		}
        
        standardTile("refresh", "device", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
		
        main(["status"])
		details(["status","refresh"])
	}
}


	
def initialize() {
	poll()
}


// handle commands
def poll() {
	log.debug "Executing 'poll'"   
    
    httpGet("https://garageio.com/api/controllers/SyncController.php?auth_token=" + data.auth.data[0].authentication_token + "&user_id=" + data.auth.userid) { response ->
        state.data = response.data       
        log.debug "Polling Result: " + state.data.success
    }
    
    if (state.data.success == false)
    {
    	log.debug "Need to login"
    	login()
    }
    else if (state.data.success == true)
    {
    	log.debug "We have a current token"
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


def actuate() {
    def currentState = state.data.data.devices[0].doors[0].state
    def changeState = (currentState == "OPEN") ? "CLOSED" : "OPEN"
    log.debug "Current State: " + currentState
    log.debug "State Change: " + changeState + " Garage"
    
    // Make sure we have credentials
    if (data.auth == null)
    {
    	login()
    }
    def params = [
        uri: "https://garageio.com/api/controllers/ToggleController.php",
        headers: [ "Content-Type": "application/x-www-form-urlencoded" ],
        body: [ 
            auth_token: data.auth.data[0].authentication_token, 
            user_id: data.auth.userid, 
            door_id: settings.door_id, 
            door_state: changeState 
        ]
    ]

    httpPost(params) { response->
        log.debug response.data
    } 
}

def login() {
    def params = [
        uri: 'https://garageio.com/api/controllers/AuthController.php',
        body: [email_address: settings.email_address, password: settings.password]
    ]

    httpPost(params) {response ->
        data.auth = response.data
        log.debug data.auth
    }
}
