/**
 *  GarageioServiceMgr
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
definition(
    name: "GarageioServiceMgr",
    namespace: "bmmiller",
    author: "Brandon Miller",
    description: "Initializes and polls Garageio device",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/bmmiller/SmartThings/master/devicetype.garageio/garageio.png",
    iconX2Url: "https://raw.githubusercontent.com/bmmiller/SmartThings/master/devicetype.garageio/garageio.png",
    iconX3Url: "https://raw.githubusercontent.com/bmmiller/SmartThings/master/devicetype.garageio/garageio.png")


preferences {
	section("About") {
		paragraph "GarageioServiceMgr, the smartapp that initializes your Garageio device and polls it on a regular basis"
		paragraph "Version 1.0\n\n" +
			"If you like this app, please support the developer via PayPal:\n\bmmiller@gmail.com\n\n" +
			"Copyright©2015 Brandon Miller"
		href url: "http://github.com/bmmiller", style: "embedded", required: false, title: "More information..."
	}

	section("Initializes and regularly polls this Garageio device") {
		input "garageio", "capability.switch", title: "Which Garageio"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	garageio.getAuthToken()
    runEvery5Minutes(takeAction)
}

def takeAction() {
	garageio.poll()	
}

def getChildName() { "Garageio Device" }

def getChildNamespace() { "bmmiller" }

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}