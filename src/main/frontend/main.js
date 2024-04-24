import Vue, {createApp} from '@vue/compat';
import BootstrapVue from 'bootstrap-vue';
import SmartgridBootstrap from './components/SmartgridBootstrap.vue';

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';

Vue.use(BootstrapVue);
const app = createApp(SmartgridBootstrap);
app.mount('#app');
