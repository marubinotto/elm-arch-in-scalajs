import { defineConfig } from 'vite'
import scalaJSPlugin from '@scala-js/vite-plugin-scalajs'

export default defineConfig({
  plugins: [scalaJSPlugin()],
  server: {
    port: 3000,
    open: true
  }
})