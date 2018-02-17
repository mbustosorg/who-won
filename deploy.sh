#!/bin/sh
git add -A
git commit -m "auto deploy"
git push origin master
git push heroku master