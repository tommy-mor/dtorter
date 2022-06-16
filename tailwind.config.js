/** @type {import('tailwindcss').Config} */
module.exports = {
	content: {
		files: ['./src/**/*.cljs', './src/**/*.clj'],
		extract: {
			wtf: content => content.match(/[^<>"'.`\s]*[^<>"'.`\s:]/g)
		}
	},
	theme: {
		extend: {},
	},
	plugins: [],
}
