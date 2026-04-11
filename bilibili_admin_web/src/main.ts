import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { restoreAuth } from './lib/auth'
import './style.css'

const app = createApp(App)

app.use(router)

restoreAuth()
app.mount('#app')
