import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { refreshCurrentUser } from './lib/auth'
import './style.css'

const app = createApp(App)

app.use(router)

refreshCurrentUser().finally(() => {
  app.mount('#app')
})
