module.exports = {
  plugins: {
    tailwindcss: {
      config: './resources/tailwind.config.js',
    },
    autoprefixer: {},
    ...(process.env.NODE_ENV === 'production' ? {
      cssnano: {},
      'postcss-hash': {
        algorithm: 'sha256',
        trim: 20,
        manifest: './target/resources/public/css/manifest.json'
      },
    } : {})
  }
}
