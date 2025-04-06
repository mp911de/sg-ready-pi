<template>
  <div class="container">
    <b-jumbotron
        lead="Current Status">
      <template #header><a href="/actuator/health">Smartgrid</a></template>

      <div>
        <b-button @click="updateRelayState('1')" :variant="getButtonVariant('1')">
          CH1: {{ getButtonLabel('1') }}
        </b-button>&nbsp;
        <b-button @click="updateRelayState('2')" :variant="getButtonVariant('2')">
          CH2: {{ getButtonLabel('2') }}
        </b-button>&nbsp;
        <b-button @click="updateRelayState('3')" :variant="getButtonVariant('3')">
          CH3: {{ getButtonLabel('3') }}
        </b-button>
      </div>
      <br/>
      <table role="table" class="table b-table table-striped table-hover">
        <thead>
        <tr>
          <td><b>Item</b></td>
          <td><b>Value</b></td>
        </tr>
        </thead>
        <tbody>
        <tr>
          <td>SG Ready State</td>
          <td>{{ health.components.debounce.details.current }}</td>
        </tr>
        <tr>
          <td>Generator</td>
          <td>{{ health.components.smaPowerGenerator.details['generator-power'] }}</td>
        </tr>
        <tr>
          <td>Battery</td>
          <td>{{ health.components.smaPowerGenerator.details['battery-soc'] }}</td>
        </tr>
        <tr>
          <td>Ingress</td>
          <td>{{ health.components.sunnyHomeManager.details.ingress }}
            ({{ health.components.sunnyHomeManager.details['ingress-momentary'] }})
          </td>
        </tr>
        <tr>
          <td>Egress</td>
          <td>{{ health.components.sunnyHomeManager.details.egress }}
            ({{ health.components.sunnyHomeManager.details['egress-momentary'] }})
          </td>
        </tr>
        <tr>
          <td>Detail</td>
          <td>
            <ul style="list-style: none">
              <li v-for="item in health.components.sgReady.details['sg-ready-decision']">
                <span v-if="item.includes('Did match')">✅</span><span v-if="!item.includes('Did match')">⚠️</span>
                {{ item }}
              </li>
            </ul>
          </td>
        </tr>
        </tbody>
      </table>

    </b-jumbotron>

  </div>
</template>
<script>
import axios from "axios";

export default {
  methods: {

    getButtonLabel(channel) {

      const state = this.relay[channel];

      if (state === 'LOW') {
        return 'closed';
      }

      if (state === 'HIGH') {
        return 'open';
      }

      return 'unknown';
    },

    getButtonVariant(channel) {

      const state = this.relay[channel];

      if (state === 'LOW') {
        return 'danger';
      }

      if (state === 'HIGH') {
        return 'success';
      }

      return 'secondary';
    },

    async getRelayStates() {
      const {data} = await axios.get("/api/relay");
      this.relay = data;
    },

    async getHealth() {
      const {data} = await axios.get("/actuator/health");

      if (!data.components.debounce) {
        data.components.debounce = {};
      }
      this.health = data;
    },

    async updateRelayState(channel) {
      var state = this.relay[channel]
      var newState = state === 'LOW' ? 'HIGH' : 'LOW'
      await axios.post("/api/relay/" + channel, newState, {
        headers: {
          'Content-Type': 'text/plain'
        }
      });
      await this.getRelayStates()
    },
  },

  data() {
    return {
      interval: undefined,

      relay: {
        "1": "",
        "2": "",
        "3": ""
      },
      health: {
        "status": "?",
        components: {
          debounce: {
            details: {
              "current": "",
              "next": "",
              "next-update": "",
              "last-update": "",
              "synchronized": true
            }
          },
          smaPowerGenerator: {
            details: {
              "generator-power": "",
              "generator-power-momentary": "",
              "battery-soc": "",
            }
          },
          sunnyHomeManager: {
            details: {
              "ingress": "",
              "ingress-momentary": "",
              "egress": "",
              "egress-momentary": "",
            }
          },
          sgReady: {
            details: {
              "sg-ready-decision": [],
            }
          }
        }
      }
    };
  },

  created() {
    this.getHealth() // first run
    this.interval = setInterval(this.getHealth, 2000)
  }
  ,

  beforeMount() {
    this.getHealth();
    this.getRelayStates();
  }
  ,
  beforeDestroy() {
    if (this.interval) {
      clearIntervall(this.interval)
      this.interval = undefined
    }
  }
  ,
}
;
</script>
