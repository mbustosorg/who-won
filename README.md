Who Won Bet Tracking
==========================

This is a simple application to help a group to keep track of March Madness bets.

This application is currently deployed at https://who-won.herokuapp.com

To run it for your own deployment you will need to set the following ENV variables:
* WHOWON_MYSQL_URL
* WHOWON_MYSQL_USER
* WHOWON_MYSQL_PASSWORD
* WHOWON_USER_PASSWORDS
* WHOWON_HOUSE_TAKE

To set these in Heroku, you can do the following:

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
```
