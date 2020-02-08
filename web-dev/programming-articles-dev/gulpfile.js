const gulp = require("gulp"),
  plumber = require("gulp-plumber");
plumberFunc = plumber({
  errorHandler(err) {
    console.log(err + "");
    this.emit("end");
  }
});
(print = require("gulp-print").default), (noop = require("gulp-noop"));

const isProd = process.argv.includes("--prod");
const dist_dir = "./dist";

function load(name, values) {
  return require(name)(values);
}

function html() {
  if(isProd) {
    throw new Error('this task is not for production');
  }
  return gulp
    .src("./dev/index.pug")
    .pipe(plumberFunc)
    .pipe(
      load("gulp-pug", {
        doctype: "html"
      })
    )
    .pipe(plumber.stop())
    .pipe(gulp.dest(dist_dir))
    .pipe(print());
}

function pcss() {
  return gulp
    .src("./dev/styles.pcss")
    .pipe(
      load("gulp-postcss", ["postcss-nested", "postcss-utilities"].map(require))
    )
    .pipe(
      load("gulp-rename", path => {
        path.extname = ".css";
      })
    )
    .pipe(isProd ? noop() : require('gulp-cssnano')())
    .pipe(gulp.dest(dist_dir))
    .pipe(print());
}

function wpcss() {
  return gulp.watch(["./dev/**/*.pcss"], pcss);
}

function js() {
  return load('browserify', {
    entries: "./dev/index.js"
  })
    .bundle()
    .pipe(load("vinyl-source-stream", "index.js"))
    .pipe(require("vinyl-buffer")())
    .pipe(isProd ? noop() : require("gulp-minify")())
    .pipe(gulp.dest(dist_dir))
    .pipe(print());
}

exports.css = pcss;
exports.pcss = pcss;
exports.wcss = wpcss;
exports.wpcss = wpcss;
exports.js = js;
exports.html = gulp.series(gulp.parallel(js, pcss), html);
