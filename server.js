var express = require('express');
var app = express();

app.set('port', (process.env.PORT || 7070));

var pageData = {
  "javascriptsBase": "/javascripts",
  "stylesheetsBase": "/stylesheets",
  "imagesBase": "/images"
};

app.use('/', express.static(__dirname + '/resources/public'));

app.set('view engine', 'jade');
app.set('views', './assets/jade');

function beforeAllFilter(req, res, next) {
  app.locals.pretty = true;

  next();
}

app.all('*', beforeAllFilter);

app.get('/', function(req, res){
  res.render('index', pageData);
});

app.get('/sign-in', function(req, res){
  res.render('sign-in', pageData);
});

app.get('/create-account', function(req, res) {
  res.render('create-account', pageData);
});


app.get('/error-500', function(req, res) {
  res.render('error-500', pageData);
});

app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});
