// Helper: root(), and rootDir() are defined at the bottom
const path = require('path');
const webpack = require('webpack');

// Webpack Plugins
const CommonsChunkPlugin = webpack.optimize.CommonsChunkPlugin;
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');

// env for dev debugging
const compileEnv = process.env.APP_ENV || 'production';
const isDev = compileEnv == 'dev';
const rootPath = '';

console.log('Webpack build in ' + (isDev ? 'dev' : 'prod')  + ' mode');
console.log('');
console.log('');

module.exports = function() {
  const config = {
    devtool: 'source-map',
    entry: {
     'polyfills': './src/polyfills.browser.ts',
     'vendor': './src/vendor.ts',
     'app': './src/main.ts'
    },
    output: {
      path: '../../../target/classes/META-INF/resources/terminal',
      publicPath: rootPath,
      filename: !isDev ? 'js/[name].[hash].js' : 'js/[name].js',
      chunkFilename: !isDev ? '[id].[hash].chunk.js' : '[id].chunk.js'
    },
    resolve: {
      unsafeCache: !isDev,
      mainFields: ['module', 'main', 'browser'],
      extensions: ['.ts', '.js', '.css', '.html', '.pug'],
      alias: {
        'jquery': path.resolve(path.join(__dirname, 'node_modules', 'jquery'))
      }
    },
    module: {
      rules: [
        {test: /\.ts$/, use:'ts-loader', exclude: [/node_modules\/(?!(ng2-.+))/]},
        {test: /\.pug$/, use:'pug-html-loader'},
        {test: /\.html$/, use:'raw-loader'},
        {test: /\.css$/, loader: ExtractTextPlugin.extract({ fallbackLoader: 'style-loader', loader: 'css-loader' })},
        {test: /jquery.terminal.+\.(js)$/, loader:'imports-loader?$=jquery,this=>window'},
        {test: /\.jpe?g$|\.gif$|\.png$|\.svg$|\.woff$|\.ttf$/, use:'file-loader'}
      ],
      noParse: [/.+zone\.js\/dist\/.+/, /.+angular2\/bundles\/.+/, /angular2-polyfills\.js/]
    }
  };

  const chunks = ['app', 'vendor', 'polyfills'];
  config.plugins = [
    new webpack.ProvidePlugin({ $: 'jquery', jQuery: 'jquery', 'window.$': 'jquery', 'window.jQuery': 'jquery' }),
    new webpack.DefinePlugin({ 'process.env': { 'compileEnv': "'" + compileEnv + "'" } }),
    new CommonsChunkPlugin({ name: chunks, minChunks: Infinity }),
    new CopyWebpackPlugin([{ from: './src/public' }]),
    new ExtractTextPlugin('app.css'),
    new HtmlWebpackPlugin({ template: './src/public/index.html', inject: 'body', chunksSortMode: (a, b) => chunks.indexOf(b.names[0]) - chunks.indexOf(a.names[0]) })
  ];

  if (!isDev) {
    config.plugins.push(
      new webpack.NoErrorsPlugin(),
      // new webpack.optimize.DedupePlugin(),
      new webpack.optimize.UglifyJsPlugin({
        beautify: false,
        comments: false,
        mangle: {
          screw_ie8 : true,
          keep_fnames: true
        },
        compress: {
          screw_ie8: true
        }
      }),
      new CompressionPlugin({
        regExp: /\.css$|\.html$|\.js$|\.map$/,
        threshold: 2 * 1024
      })
    );
  }

  return config;
}();
