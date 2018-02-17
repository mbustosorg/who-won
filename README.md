Who Won Bet Tracking
==========================

[![Build Status](https://travis-ci.org/mbustosorg/who-won.svg?branch=master)](https://travis-ci.org/mbustosorg/who-won)

This is a simple application to help a group to keep track of March Madness bets.

This application is currently deployed at https://who-won.herokuapp.com

To run it for your own deployment you will need to set the following ENV variables:
* WHOWON_MYSQL_URL
* WHOWON_MYSQL_USER
* WHOWON_MYSQL_PASSWORD
* WHOWON_USER_PASSWORDS
* WHOWON_HOUSE_TAKE
* AWS_ACCESS_KEY_ID
* AWS_SECRET_ACCESS_KEY
* GOOGLE_APPLICATION_CREDENTIALS_FILE
* GOOGLE_APPLICATION_CREDENTIALS

The AWS variables are to allow the application to store the images of betting slips on S3 for bet validation and future model training.

The Google App variables are to allow the application to use the Google Cloud Vision APIs to OCR the betting slips.
You need to create a service account on Google in order for OCR to work.  Otherwise you can still use manual bet entry.

To set these in Heroku, you do the following:

```bash
$ heroku config:set WHOWON_MYSQL_URL=jdbc:mysql://mysql.*****:3306/whowon
Setting config vars and restarting who-won... done
WHOWON_MYSQL_URL: jdbc:mysql://*****:3306/whowon
$ heroku config:set WHOWON_MYSQL_USER=whowonuser
Setting config vars and restarting who-won... done
WHOWON_MYSQL_USER: whowonuser
$ heroku config:set WHOWON_MYSQL_PASSWORD=*****
Setting config vars and restarting who-won... done
WHOWON_MYSQL_PASSWORDS: *****
$ heroku config:set WHOWON_USER_PASSWORDS="user1,pass1;user2,pass2"
Setting config vars and restarting who-won... done
WHOWON_USER_PASSWORDS: *****
$ heroku config:set WHOWON_HOUSE_TAKE="0.0455"
Setting config vars and restarting who-won... done
WHOWON_HOUSE_TAKE: 0.0455
$ heroku config:set AWS_ACCESS_KEY_ID=****
Setting AWS_ACCESS_KEY_ID and restarting ⬢ who-won... done, v44
AWS_ACCESS_KEY_ID: ****
$ heroku config:set AWS_SECRET_ACCESS_KEY=****
Setting AWS_SECRET_ACCESS_KEY and restarting ⬢ who-won... done, v45
AWS_SECRET_ACCESS_KEY: ****
$ heroku config:set GOOGLE_APPLICATION_CREDENTIALS_FILE="$(< ******.json)"
Setting GOOGLE_APPLICATION_CREDENTIALS_FILE and restarting ⬢ who-won... done, v62
$ heroku config:set GOOGLE_APPLICATION_CREDENTIALS="/tmp/google_application_credentials.json"
Setting GOOGLE_APPLICATION_CREDENTIALS and restarting ⬢ who-won... done, v67
GOOGLE_APPLICATION_CREDENTIALS: /tmp/google_application_credentials.json
```
