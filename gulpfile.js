var gulp = require('gulp'),
    jade = require('gulp-jade'),
    sass = require('gulp-sass'),
    gulpif = require('gulp-if'),
    autoprefixer = require('gulp-autoprefixer'),
    minifyCSS = require('gulp-minify-css'),
    imagemin = require('gulp-imagemin'),
    del = require('del'),
    runSequence = require('run-sequence'),
    html5Lint = require('gulp-html5-lint'),
    browsersync = '',
    nodemon = '',
    ghPages = '';

var isDev = false;
var output_path = 'resources/public';
var deployed_path = 'deployed';
var dev_path = {
  sass: ['assets/stylesheets/*.scss', '!assets/stylesheets/_*.scss'],
  jadedev: ['assets/jade/**/*.jade', '!assets/jade/_*.jade'],
  jade: ['assets/jade/**/*.jade', '!assets/jade/_*.jade', '!assets/jade/layout/*', '!assets/jade/mixins/*'],
  js: ['assets/javascripts/**/*.js'],
  images: ['assets/images/**/*', '!assets/images/dev-*'],
  favicons: ['assets/icons/favicon.*'],
  fonts: ['assets/stylesheets/fonts/*','/bower_components/font-awesome/fonts/*'],
  port: 5678
};
var build_path = {
  css: output_path + '/stylesheets/',
  html: output_path + '/',
  js: output_path + '/javascripts/',
  images: output_path + '/images',
  fonts: output_path + '/stylesheets/fonts/',
  library: output_path + '/library/'
};

gulp.task('jade', function () {
  var jadeSrc = isDev ? dev_path.jadedev : dev_path.jade;
  return gulp.src(jadeSrc)
      .pipe(jade({
        pretty: true,
        locals: {
          "javascriptsBase": "javascripts",
          "stylesheetsBase": "stylesheets",
          "imagesBase": "images",
          "initData": ""
        }
      }))
      .on('error', function(err) {
        console.log(err);
        this.emit('end');
      })
      .pipe(gulp.dest(build_path.html));
});

gulp.task('sass', function () {
  return gulp.src(dev_path.sass)
      .pipe(sass({style: 'expanded', errLogToConsole: true}))
      .on('error', function(err) {
        console.log(err);
        this.emit('end');
      })
      .pipe(autoprefixer())
      .pipe(gulpif(!isDev, minifyCSS({noAdvanced: true}))) // minify if Prod
      .pipe(gulp.dest(build_path.css));
});

gulp.task('js', function () {
  return gulp.src(dev_path.js)
      .on('error', function(err) {
        console.log(err);
        this.emit('end');
      })
      .pipe(gulp.dest(build_path.js));
});

gulp.task('images', function () {
  return gulp.src(dev_path.images)
      .pipe(imagemin({optimizationLevel: 3}))
      .pipe(gulp.dest(build_path.images));
});

gulp.task('favicons', function () {
  return gulp.src(dev_path.favicons)
      .pipe(imagemin({optimizationLevel: 3}))
      .pipe(gulp.dest(build_path.html));
});

gulp.task('fonts', function () {
  return gulp.src(dev_path.fonts)
      .pipe(gulp.dest(build_path.fonts));
});

gulp.task('html5lint', ['jade'], function() {
  return gulp.src(output_path + '/*.html')
      .pipe(html5Lint());
});

// Reload all Browsers
gulp.task('bs-reload', function () {
  browsersync.reload();
});

gulp.task('browser-sync', ['nodemon'], function () {
  return browsersync.init(null, {
    proxy: "localhost:7070",  // local node app address
    port: dev_path.port,  // use *different* port than above
    notify: true,
    open: false,
    files: [build_path.images,
      dev_path.jade,
      dev_path.favicons,
      dev_path.js,
      build_path.css]
  })
});

gulp.task('clean-build', function (cb) {
  del([output_path], cb);
});
gulp.task('clean-deployed', function (cb) {
  del([deployed_path], cb);
});

gulp.task('watch', function () {
  gulp.watch('assets/jade/**/*.jade', browsersync.reload);
  gulp.watch('assets/stylesheets/**/*.scss', ['sass']);
  gulp.watch(dev_path.images, ['images']);
  gulp.watch(dev_path.js, ['js']);
});

gulp.task('server', function (callback) {
  isDev = true;
  browsersync = require('browser-sync').create();
  nodemon = require('gulp-nodemon');

  runSequence('clean-build',
      ['sass', 'js', 'images', 'favicons', 'fonts', 'browser-sync', 'watch'],
      callback);
});

gulp.task('build', function (callback) {
  runSequence('clean-build',
      ['jade', 'sass', 'js', 'images', 'favicons', 'fonts'],
      callback);
});

gulp.task('ghpages', function() {
  return gulp.src('./resources/public/**/*')
      .pipe(ghPages({cacheDir:deployed_path}));
});

gulp.task('deploy', function (callback) {
  ghPages = require('gulp-gh-pages');
  runSequence(['build'],
      ['ghpages'],
      ['clean-deployed'], callback);
});

gulp.task('nodemon', function (cb) {
  var called = false;
  return nodemon({
    script: 'server.js',
    ignore: [
      'Gulpfile.js',
      'node_modules/'
    ]
  })
  .on('start', function () {
    if (!called) {
      called = true;
      cb();
    }
  })
  .on('restart', function () {
    setTimeout(function () {
      browsersync.stream();
    }, 1000);
  });
});

gulp.task('default', function () {
  gulp.start('server');
});
