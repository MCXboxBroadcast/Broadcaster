/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {}
  },
  plugins: [],
  safelist: [
    {
      pattern: /bg-(red|green)-(3|7|8)00/,
      variants: ['hover']
    }
  ]
}
