/**
 *  Log to Graphite
 *
 *  Copyright 2014 Brian Keifer
 *	https://github.com/bkeifer/smartapp.log-to-graphite/blob/master/log-to-graphite.smartapp.groovy
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
    name: "Log to Graphite",
    namespace: "bkeifer",
    author: "Brian Keifer/Brandon Miller",
    description: "Log various things to a Graphite instance",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

	section("Log devices...") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required:false, multiple: true
        input "contacts", "capability.contactSensor", title: "Contacts", required: false, multiple: true
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminances", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "batteries", "capability.battery", title: "Batteries", required:false, multiple: true
        input "thermostats", "capability.thermostat", title: "Thermostat Setpoints", required: false, multiple: true
        input "energymeters", "capability.powerMeter", title: "Power Meters", required: false, multiple: true
        input "voltagemeters", "capability.voltageMeter", title: "Voltage Meters", required: false, multiple: true
    }
    section ("Graphite (Backstop) Server") {
    	input "backstop_host", "text", title: "Backstop Hostname/IP"
        input "backstop_port", "number", title: "Backstop Port"
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
	state.clear()
    unschedule(checkSensors)
    //schedule("* * * * * ?", "checkSensors")
    subscribe(app, appTouch)
}


def appTouch(evt) {
	log.debug "Manually triggered: $evt"
    checkSensors()
	logURLs()
}


def checkSensors() {

    def logitems = []

    for (t in settings.temperatures) {
        logitems.add([t.displayName, "smartthings.temperature", Double.parseDouble(t.latestValue("temperature").toString())] )
        state[t.displayName + ".temp"] = t.latestValue("temperature")
        log.debug("[temp]     " + t.displayName + ": " + t.latestValue("temperature"))
    }
    for (t in settings.humidities) {
        logitems.add([t.displayName, "smartthings.humidity", Double.parseDouble(t.latestValue("humidity").toString())] )
        state[t.displayName + ".humidity"] = t.latestValue("humidity")
		log.debug("[humidity] " + t.displayName + ": " + t.latestValue("humidity"))
	}
    for (t in settings.batteries) {
        logitems.add([t.displayName, "smartthings.battery", Double.parseDouble(t.latestValue("battery").toString())] )
        state[t.displayName + ".battery"] = t.latestValue("battery")
		log.debug("[battery]  " + t.displayName + ": " + t.latestValue("battery"))
	}

    for (t in settings.contacts) {
        logitems.add([t.displayName, "smartthings.contact", t.latestValue("contact")] )
        state[t.displayName + ".contact"] = t.latestValue("contact")
    }

    for (t in settings.motions) {
        logitems.add([t.displayName, "smartthings.motion", t.latestValue("motion")] )
        state[t.displayName + ".motion"] = t.latestValue("motion")
    }

    for (t in settings.illuminances) {
        def x = new BigDecimal(t.latestValue("illuminance") ) // instanceof Double)
        logitems.add([t.displayName, "smartthings.illuminance", x] )
        state[t.displayName + ".illuminance"] = x
		log.debug("[luminance] " + t.displayName + ": " + t.latestValue("illuminance"))
	}

    for (t in settings.switches) {
        logitems.add([t.displayName, "smartthings.switch", (t.latestValue("switch") == "on" ? 1 : 0)] )
        state[t.displayName + ".switch"] = (t.latestValue("switch") == "on" ? 1 : 0)
        log.debug("[switch] " + t.displayName + ": " + (t.latestValue("switch") == "on" ? 1 : 0))
    }

    for (t in settings.thermostats) { 
        
    	if (t?.latestValue("heatingSetpoint") > 0)
        {
        	logitems.add([t.displayName, "smartthings.thermostat.heatingSetPoint", t.latestValue("heatingSetpoint")] )
        	state[t.displayName + ".setPoint"] = t.latestValue("heatingSetpoint")
        	log.debug("[thermostat] " + t.displayName + ": " + t.latestValue("heatingSetpoint"))
        }
        else if (t?.latestValue("heatingSetpoint") == 0 && t?.latestValue("coolingSetpoint") > 0) // Cooling only mode
        {
        	logitems.add([t.displayName, "smartthings.thermostat.setPoint", t.latestValue("coolingSetpoint")] )
        	state[t.displayName + ".setPoint"] = t.latestValue("coolingSetpoint")
        	log.debug("[thermostat] " + t.displayName + ": " + t.latestValue("coolingSetpoint"))
        }
        
        if (t?.latestValue("coolingSetpoint") > 0)
        {
        	logitems.add([t.displayName, "smartthings.thermostat.coolingSetPoint", t.latestValue("coolingSetpoint")] )
        	state[t.displayName + ".setPoint"] = t.latestValue("coolingSetpoint")
        	log.debug("[thermostat] " + t.displayName + ": " + t.latestValue("coolingSetpoint"))
        }  
        else if (t?.latestValue("coolingSetpoint") == 0 && t?.latestValue("heatingSetpoint") > 0) // Heating only mode
        {
        	logitems.add([t.displayName, "smartthings.thermostat.setPoint", t.latestValue("heatingSetpoint")] )
        	state[t.displayName + ".setPoint"] = t.latestValue("heatingSetpoint")
        	log.debug("[thermostat] " + t.displayName + ": " + t.latestValue("heatingSetpoint"))
        }
        
        if (t?.latestValue("humiditySetpoint") > 0)
        {
        	logitems.add([t.displayName, "smartthings.thermostat.humiditySetPoint", t.latestValue("humiditySetpoint")] )
        	state[t.displayName + ".humiditySetPoint"] = t.latestValue("humiditySetpoint")
       		log.debug("[thermostat] " + t.displayName + ": " + t.latestValue("humiditySetpoint") )
        }
    }

    for (t in settings.energymeters) {
        logitems.add([t.displayName + ".power", "smartthings.energy", t.latestValue("power")])
        state[t.displayName + ".Watts"] = t.latestValue("power")
        log.debug("[energy] " + t.displayName + ": " + t.latestValue("power"))
    }
    
    for (t in settings.voltagemeters) {
		logitems.add([t.displayName + ".volts", "smartthings.energy", t.latestValue("volts")])
        state[t.displayName + ".Volts"] = t.latestValue("volts")
        log.debug("[energy] " + t.displayName + ": " + t.latestValue("volts"))
    }

	logField2(logitems)

}


private logField2(logItems) {
    def fieldvalues = ""
    def timeNow = now()
    timeNow = (timeNow/1000).toInteger()

    logItems.eachWithIndex() { item, i ->
		def path = item[0].replace(" ","")
		def value = item[2]

		def json = "{\"metric\":\"${path}\",\"value\":\"${value}\",\"measure_time\":\"${timeNow}\"}"
		log.debug json

		def params = [
            headers: [ HOST: "${backstop_host}:${backstop_port}"],
            path: "/publish/${item[1]}",
            body: json           
        ]
        try {
        	log.debug "${params}"          
            sendHubCommand(new physicalgraph.device.HubAction(params))
            log.debug "Success!"
        }
		catch ( groovyx.net.http.HttpResponseException ex ) {
        	log.debug "Unexpected response error: ${ex.statusCode}"
        }
	}
}

mappings {
  path("/stamp") {
    action: [
      GET: "checkStamp",
    ]
  }
  path("/reschedule") {
    action: [
      GET: "reschedule",
    ]
  }
  path("/touch") {
  	action: [
      GET: "appTouch",
    ]
  }
}


def updateState() {
	log.debug("State updated!")
    state.timestamp = now()
}


def createSchedule() {
    unschedule()
    updateState()
    //schedule("0 * * * * ?", "updateState")
}

def logURLs() {
	if (!state.accessToken) {
		try {
			createAccessToken()
			log.debug "Token: $state.accessToken"
		} catch (e) {
			log.debug("Error.  Is OAuth enabled?")
		}
	}
    def baseURL = "https://graph.api.smartthings.com/api/smartapps/installations"
	log.debug "Stamp URL:  ${baseURL}/${app.id}/stamp?access_token=${state.accessToken}"
	log.debug "Reschedule URL:  ${baseURL}/${app.id}/reschedule?access_token=${state.accessToken}"
    log.debug "Touch URL:  ${baseURL}/${app.id}/touch?access_token=${state.accessToken}"
}

def checkStamp() {
    def result
    def resultcode
    // 300000 = 5 minutes in milliseconds.  Replace with a value of at least
    // 2x the frequency at which your scheduled function should run.
    if (now() - state.timestamp < 300000) {
        result = "FIRING"
        resultcode = 1
    } else {
        result = "FAIL"
        resultcode = 0
    }
    def resp = []
    resp << [ app_name: app.name, status: result, status_code: resultcode, last_timestamp: new Date(state.timestamp) ]
    return resp
}


def reschedule() {
  createSchedule()
  log.trace("Rescheduled via web API call!")
  appTouch()
  def resp = []
  resp << [ app_name: app.name, job: 'RESCHEDULE', status: 'SUCCESS' ]
  return resp
}