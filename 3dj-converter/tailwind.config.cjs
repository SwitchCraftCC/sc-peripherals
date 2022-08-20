/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx,ts,tsx}"
  ],
  theme: {
    container: {
      screens: {
        sm: "100%",
        md: "100%",
        lg: "920px",
        xl: "1024px"
      }
    },

    extend: {},
  },
  plugins: []
}
