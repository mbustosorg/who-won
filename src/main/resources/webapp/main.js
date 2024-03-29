/*

    Copyright (C) 2016 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

$(document).ready(function() {

    var iphone = false;
    if((navigator.userAgent.match(/iPhone/i)) || (navigator.userAgent.match(/iPod/i))) {
        if (document.cookie.indexOf("iphone_redirect=false") == -1) {
            iphone = true;
        }
    }
    if (iphone) {
        $('#mobilePhoto').removeClass('hide');
    } else {
        $('#desktopPhoto').removeClass('hide');
        $('#snapButtons').removeClass('hide');
    }

    function year() {
        var year = $('#yearDropdownLabel').text();
        if (year == 'Year ') {
           year = (new Date()).getFullYear();
        } else if (year == 'All') {
           year = 0;
        }
        return year;
    }

	var video = document.getElementById("snapVideo");
    var constraints = { audio: false, video: true };
    var webcamStream = null;

    var queryingCompetitors = false;
    var displayingCompetitors = false;

    function startVideo() {
        if (webcamStream == null) {
            function errBack(message) {
                window.alert("Make sure you are accessing this site using https & grant access to your camera for this site.")
                console.log("Video capture error: ", message);
            };
            if(navigator.getUserMedia) { // Standard
                navigator.mediaDevices.getUserMedia(constraints)
                  .then(function(stream) {
                    var videoTracks = stream.getVideoTracks();
                    console.log('Got stream with constraints:', constraints);
                    console.log('Using video device: ' + videoTracks[0].label);
                    stream.onended = function() {
                      console.log('Stream ended');
                    };
                    stream.oninactive = function() {
                      console.log('Stream inactive');
                    };
                    window.stream = stream; // make variable available to console
                    video.srcObject = stream;
                    webcamStream = stream;
                  })
                  .catch(errBack);
/*
                navigator.getUserMedia(videoObj, function(stream) {
                    webcamStream = stream;
                    video.src = stream;
                    video.play();
                }, errBack);
                */
            } else if(navigator.webkitGetUserMedia) { // WebKit-prefixed
                navigator.webkitGetUserMedia(videoObj, function(stream){
                    webcamStream = stream;
                    video.src = window.URL.createObjectURL(stream);
                    video.play();
                }, errBack);
            }
            else if(navigator.mozGetUserMedia) { // Firefox-prefixed
                navigator.mozGetUserMedia(videoObj, function(stream){
                    webcamStream = stream;
                    video.src = window.URL.createObjectURL(stream);
                    video.play();
                }, errBack);
            }
        }
    }

    function stopVideo() {
        if (webcamStream != null) {
            webcamStream.getTracks()[0].stop();
            webcamStream = null;
        }
    }

    $('#myNavbar a').on('click', function(){
        $('.navbar-toggle').click()
    });

    function stopSnap(img) {
        $('#snapImage').remove();
        $('#photoPage').prepend(img);
        stopVideo();
    };

    function prepareForSnap() {
        $('#snapImage').remove();
        $('video').removeClass('hide');
        $('#runningQuery').addClass('hide');
        startVideo();
    };

    $('#snapButton').click(function() {
        $('video').addClass('hide');

/*
        var h2 = video.videoHeight / video.videoWidth * $('#photoPage')[0].offsetWidth;
        var w2 = $('#photoPage')[0].offsetWidth;

        var newCanvas = document.createElement("canvas");
        newCanvas.width = video.videoWidth;
        newCanvas.height = video.videoHeight;
        newCanvas.getContext('2d').drawImage(video, 0, 0, newCanvas.width, newCanvas.height);

        var img = new Image();
        img.src = newCanvas.toDataURL();
        img.id = 'snapImage';
        img.height = h2;
        img.width = w2;
        */

        var h2 = video.videoHeight / video.videoWidth * $('#photoPage')[0].offsetWidth;
        var w2 = $('#photoPage')[0].offsetWidth;

        var newCanvas = document.createElement("canvas");
        newCanvas.width = video.videoWidth;
        newCanvas.height = video.videoHeight;
        newCanvas.getContext('2d').transform(0, 1, -1, 0, newCanvas.width, 0);
        newCanvas.getContext('2d').drawImage(video, 0, 0, newCanvas.height, newCanvas.width);

        var img = new Image();
        img.src = newCanvas.toDataURL();
        img.id = 'snapImage';
        img.height = w2;
        img.width = h2;

        stopSnap(img);
    });

    $('#snapRetakeButton').click(function() {
        prepareForSnap();
    });

    $('#mobileInputPhoto').change(function () {
      if (this.files && this.files[0]) {
        var reader = new FileReader();
        reader.onload = function (e) {
            if ($('#snapImage') != null) $('#snapImage').remove();
            var img = new Image();
            img.src = e.target.result;
            img.onload = function () {
                var h2 = img.naturalHeight / img.naturalWidth * $('#photoPage')[0].offsetWidth;
                var w2 = $('#photoPage')[0].offsetWidth;

                var scale = 0.3
                var newCanvas = document.createElement("canvas");
                newCanvas.width = img.naturalWidth * scale;
                newCanvas.height = img.naturalHeight * scale;
                newCanvas.getContext('2d').transform(0, 1, -1, 0, newCanvas.width, 0);
                newCanvas.getContext('2d').transform(scale, 0, 0, scale, 0, 0);
                newCanvas.getContext('2d').drawImage(img, 0, 0, newCanvas.height / scale, newCanvas.width / scale);

                var scaledImage = new Image();
                scaledImage.src = newCanvas.toDataURL();
                scaledImage.id = 'snapImage';
                scaledImage.height = w2;
                scaledImage.width = h2;

                stopSnap(scaledImage);
            }
        };
        reader.readAsDataURL(this.files[0]);
      }
    });

    $('#straightBetNav').click(function() {
        $('#manualEntryTab').removeClass('hide');
        $('#photoPage').addClass('hide');
        stopVideo();
    });
    $('#moneyLineBetNav').click(function() {
        $('#manualEntryTab').removeClass('hide');
        $('#photoPage').addClass('hide');
        stopVideo();
    });
    $('#scanBetNav').click(function() {
        $('#manualEntryTab').addClass('hide');
        $('#photoPage').removeClass('hide');
        startVideo();
    });

    $('#entryPageNav').click(function() {
        $('#navbar').removeClass('in');
        $('#betEntry').removeClass('hide');
        $('#report').addClass('hide');
        $('#admin').addClass('hide');
        $('#entryPageNav').addClass('active');
        $('#reportPageNav').removeClass('active');
        $('#adminPageNav').removeClass('active');
        $('#logoutNav').removeClass('active');
    });
    $('#reportPageNav').click(function() {
        $('#navbar').removeClass('in');
        $('#betEntry').addClass('hide');
        $('#report').removeClass('hide');
        $('#admin').addClass('hide');
        $('#entryPageNav').removeClass('active');
        $('#reportPageNav').addClass('active');
        $('#adminPageNav').removeClass('active');
        $('#logoutNav').removeClass('active');
        updateWinnings();
    });
    $('#adminPageNav').click(function() {
        $('#navbar').removeClass('in');
        $('#report').addClass('hide');
        $('#betEntry').addClass('hide');
        $('#admin').removeClass('hide');
        $('#reportPageNav').removeClass('active');
        $('#entryPageNav').removeClass('active');
        $('#adminPageNav').addClass('active');
        $('#resultsRunningQuery').removeClass('hide');
        $('#logoutNav').removeClass('active');
        updateMissingResults();
    });
    $('#logoutNav').click(function() {
        $('#betEntry').addClass('hide');
        $('#report').addClass('hide');
        $('#admin').addClass('hide');
        $('#entryPageNav').removeClass('active');
        $('#reportPageNav').removeClass('active');
        $('#adminPageNav').removeClass('active');
        $('#logoutNav').addClass('active');
        $.ajax({
          type: "POST",
          url: "/logout",
          success: function(data){
            var http = location.protocol;
            var slashes = http.concat("//");
            var host = slashes.concat(window.location.host);
            window.location.replace(host.concat(data));
            window.location.reload();
          }
        });
    });

    $('#opt-sb-over').click(function() {
        updateSpread(50.0, 300.0, 130.0);
        $('#spread-label').text('Total Points');
    });
    $('#opt-sb-under').click(function() {
        updateSpread(50.0, 300.0, 130.0);
        $('#spread-label').text('Total Points');
    });

    $('#opt-sb-game').click(function() {
        updateSpread(-40.0, 40.0, 0.0);
        $('#spread-label').text('Spread');
    });

    var opposingGames = {}

    $('#resultsBookId').change(function() {
        var bookId = $('#resultsBookId').val().split(' ')[0];
        var opposingGame = opposingGames[bookId];
        $('#resultsOpposingBookId').empty();
        $('#firstTo15').empty();
        var labelString = opposingGame.bookId + ' - ' + opposingGame.teamName;
        $('#resultsOpposingBookId').append(
            '<option value =\"' + labelString + '\">' + labelString + '</option>'
        );
        $('#firstTo15').append(
            '<option value =\"' + $('#resultsBookId').val() + '\">' + $('#resultsBookId').val() + '</option>'
        );
        $('#firstTo15').append(
            '<option value =\"' + labelString + '\">' + labelString + '</option>'
        );
    });

    function updateMissingResults() {
        $.ajax({
            url: '/games/' + year() + '/missing'
        }).done(function(results) {
            opposingGames = {};
            $('#resultsBookId').empty();
            $('#resultsOpposingBookId').empty();
            $('#firstTo15').empty();
            $('#resultsBookId').append('<option value =\"\"></option>');
            $.each(results, function(key, currentGame) {
                opposingGames[currentGame.opposingBookId] = currentGame;
                var labelString = currentGame.bookId + ' - ' + currentGame.teamName;
                $('#resultsBookId').append(
                    '<option value =\"' + labelString + '\">' + labelString + '</option>'
                );
            });
            $('#selectedScore').empty();
            $('#selectedScore').append('<option value =\"0\">0</option>');
            $('#selectedFirstHalfScore').empty();
            $('#selectedFirstHalfScore').append('<option value =\"0\">0</option>');
            $('#opposingScore').empty();
            $('#opposingScore').append('<option value =\"0\">0</option>');
            $('#opposingFirstHalfScore').empty();
            $('#opposingFirstHalfScore').append('<option value =\"0\">0</option>');
            for (i = 20; i < 150; i = i + 1) {
                $('#selectedScore').append(
                    '<option value =\"' + i + '\">' + i + '</option>'
                );
                $('#opposingScore').append(
                    '<option value =\"' + i + '\">' + i + '</option>'
                );
                $('#selectedFirstHalfScore').append(
                    '<option value =\"' + i + '\">' + i + '</option>'
                );
                $('#opposingFirstHalfScore').append(
                    '<option value =\"' + i + '\">' + i + '</option>'
                );
            }
            $('#resultsRunningQuery').addClass('hide');
        });
    }

    function updateWinnings() {
        if (!$('#report').hasClass('hide')) {
            $('#reportQuery').removeClass('hide');
            $.ajax({
                url: '/betProfiles/' + year(),
                cache: false
            }).done(function(results) {
                var data = new google.visualization.DataTable();
                data.addColumn('number', 'Bet Size');
                $.each(results.values, function(key, currentBet) {
                    var countOfBets = Number(currentBet[1]);
                    for (i = 0; i < countOfBets; i++) {
                        data.addRow([Number(currentBet[0])]);
                    }
                });
                var options = {
                  title: 'Bet Distribution (count vs $)',
                  chartArea: { left: 60, top: 30, width: '85%', height: '80%'},
                  legend: { position: 'none' },
                  histogram: { bucketSize: 10 }
                };
                var chart = new google.visualization.Histogram(document.getElementById('betSizeHistogram'));
                chart.draw(data, options);
                $('tbody#bet_type_table_body').empty();
                $.each(results.largest, function(key, currentBet) {
                    $('#bet_type_table_body').append(
                        '<tr>' +
                        '<td>Largest</td>' +
                        '<td>' + currentBet.userName + '</td>' +
                        '<td>$' + currentBet.amount + '</td>' +
                        '</tr>'
                    );
                });
                $.each(results.smallest, function(key, currentBet) {
                    $('#bet_type_table_body').append(
                        '<tr>' +
                        '<td>Smallest</td>' +
                        '<td>' + currentBet.userName + '</td>' +
                        '<td>$' + currentBet.amount + '</td>' +
                        '</tr>'
                    );
                });
            });
            $.ajax({
                url: '/winnings/' + year(),
                cache: false
            }).done(function(results) {
                var data = new google.visualization.DataTable();
                data.addColumn('date', 'Time');
                var roiData = new google.visualization.DataTable();
                roiData.addColumn('date', 'Time');
                for (i = 0; i < results.list.length; i++) {
                  data.addColumn('number', results.list[i].userName);
                  roiData.addColumn('number', results.list[i].userName);
                }
                for (i = 0; i < results.timestamps.length; i++) {
                  var row = [new Date(results.timestamps[i])];
                  var pctRow = [new Date(results.timestamps[i])];
                  var nonZero = false;
                  for (j = 0; j < results.list.length; j++) {
                    row[j + 1] = Number(results.list[j].winnings[i].toFixed(2));
                    //pctRow[j + 1] = Number((results.list[j].roi[i] * 100.0).toFixed(2));
                    pctValue = Number((results.list[j].winningsLive[i]).toFixed(2));
                    if (pctValue != 0.0) nonZero = true;
                    pctRow[j + 1] = Number((results.list[j].winningsLive[i]).toFixed(2));
                  }
                  data.addRow(row);
                  if (nonZero) roiData.addRow(pctRow);
                }
                var options = {
                  title: 'Wallet ($ vs time)',
                  legend: { position: 'bottom' },
                  chartArea: { left: 60, top: 30, width: '85%', height: '80%'},
                  hAxis: {
                    title: 'Time',
                    ticks: []
                  }
                };
                var chart = new google.visualization.LineChart(document.getElementById('winningsChart'));
                chart.draw(data, options);
                var options = {
                  title: 'Timestamped Winnings ($ vs time)',
                  legend: { position: 'bottom' },
                  chartArea: { left: 60, top: 30, width: '85%', height: '80%'},
                  hAxis: {
                    title: 'Time',
                    ticks: []
                  },
                };
                var chart = new google.visualization.LineChart(document.getElementById('percentageWinChart'));
                chart.draw(roiData, options);
                $('#reportQuery').addClass('hide');
            });
            setTimeout(updateWinnings, 60000);
        }
    };

    function getBase64Image(imgElem) {
        var canvas = document.createElement("canvas");
        canvas.width = imgElem.naturalWidth;
        canvas.height = imgElem.naturalHeight;
        var ctx = canvas.getContext("2d");
        ctx.drawImage(imgElem, 0, 0, canvas.width, canvas.height, 0, 0, canvas.height, canvas.width);
        var dataURL = canvas.toDataURL("image/png");
        return dataURL.replace(/^data:image\/(png|jpg);base64,/, "");
    };

    function nameForBetType(betType) {
        if (betType == 'ML') return 'Moneyline';
        else if (betType == 'ST') return 'Straight Bet';
        else if (betType == 'ST-1H') return 'Straight 1st';
        else if (betType == 'ML-OV') return 'Over';
        else if (betType == 'ML-OU') return 'Under';
        else if (betType == 'ML-15') return '1st to 15';
        else if (betType == 'ML-1H') return '1st Half';
        else return 'Unknown';
    };

    $('#betSubmit').click(function() {
        if ($('#photoPage').hasClass('hide')) submitBet();
        else {
           $('#runningQuery').removeClass('hide');
           var imgData = JSON.stringify(getBase64Image($('#snapImage')[0]));
            $.ajax({
                type: "POST",
                url: '/ticket',
                contentType: 'application/json',
                data: imgData
            }).done(function(results) {
                var result = '<p>Book Id: ' + results.bookId +
                             '</p><p>Amount: $' + results.amount +
                             '</p><p>Bet Type: ' + nameForBetType(results.betType) + '</p>';
                if (results.year > 0 && results.year != parseInt(year())) {
                    $('#modal-scan-text').html('Ticket invalid for selected year.');
                    $('#confirm-modal').modal('show');
                    $('#confirm-scan-button').click(function() {
                        $('#confirm-modal').modal('hide');
                    });
                } else if (results.betType == 'UNKNOWN' || results.bookId == 0 || results.spread_ml == 0 || results .amount == 0) {
                    $('#modal-scan-text').html('Unable to decode image.  Retake image or enter manually.');
                    $('#confirm-modal').modal('show');
                    $('#confirm-scan-button').click(function() {
                        $('#confirm-modal').modal('hide');
                    });
                } else {
                    if (results.betType == 'ST') result = result + '<p>Spread: ' + results.spread_ml + '</p>';
                    else result = result + '<p>Moneyline: ' + results.spread_ml + '</p>';
                    $('#modal-scan-text').html(result);
                    $('#confirm-modal').modal('show');
                    $('#confirm-scan-button').click(function() {
                        $('#confirm-modal').modal('hide');
                        sendBetToServer(results.bookId, results.spread_ml, results.amount, results.betType)
                    });
                }
                prepareForSnap();
            });
        }
    });

    $('#resultsSubmit').click(function() {
        var bookId = $('#resultsBookId').val().split(' ')[0];
        var selectedScore = $('#selectedScore').val();
        var selectedFirstHalfScore = $('#selectedFirstHalfScore').val();
        var opposingBookId = $('#resultsOpposingBookId').val().split(' ')[0];
        var opposingScore = $('#opposingScore').val();
        var opposingFirstHalfScore = $('#opposingFirstHalfScore').val();
        var firstTo15 = $('#firstTo15').val().split(' ')[0];
        var timestamp = (new Date()).toISOString();
        if (bookId == opposingBookId) {
        } else {
            $('#resultsRunningQuery').removeClass('hide');
            var favFirst = true;
            if (firstTo15 != bookId) favFirst = false;
            var gameResult = '{\"year\": ' +
                year() + ', \"bookId\": ' + bookId +
                ', \"finalScore\": ' + selectedScore +
                ', \"firstHalfScore\": ' + selectedFirstHalfScore +
                ', \"opposingBookId\": ' + opposingBookId +
                ', \"opposingFinalScore\": ' + opposingScore +
                ', \"opposingFirstHalfScore\": ' + opposingFirstHalfScore +
                ', \"resultTimeStamp\": \"' + timestamp + '\"' +
                ', \"firstTo15\": ' + favFirst + '}';
            $.ajax({
                type: "POST",
                url: '/games/' + year(),
                contentType: 'application/json',
                data: gameResult
            }).done(function(results) {
                $('#resultsRunningQuery').addClass('hide');
                updateMissingResults();
                displayGameResults();
                $('#resultsSubmitResult').addClass('alert-success');
                $('#resultsSubmitResult').removeClass('alert-danger');
                $('#resultsSubmitResult').html('<strong>' + results.responseText + '</strong>');
            });
        }
    });

    function submitBet() {
        $('#runningQuery').removeClass('hide');
        var bookId = $('#bookId').val().split(' ')[0];
        var spreadMlAmount = '0';
        var betAmount = $('#betAmount').val();
        var betType = '';
        if ($('#moneylinePane').hasClass('active')) {
            betType = 'ML';
            if ($('#opt-ml-first-half')[0].checked) {
                betType = betType + '-1H';
            } else if ($('#opt-ml-first-to-15')[0].checked) {
                betType = betType + '-15';
            }
            spreadMlAmount = $('#moneyline').val();
        } else {
            betType = 'ST';
            if ($('#opt-sb-over')[0].checked) {
                betType = betType + '-OV';
            } else if ($('#opt-sb-under')[0].checked) {
                betType = betType + '-UN';
            } else if ($('#opt-sb-first-half')[0].checked) {
                betType = betType + '-1H';
            }
            spreadMlAmount = $('#spreadAmount').val();
        }
        sendBetToServer(bookId, spreadMlAmount, betAmount, betType);
    };

    function sendBetToServer(bookId, spreadMlAmount, betAmount, betType) {
        var userName = getCookie("WHOWON_USER");
        var dataString = '{\"userName\": \"' + userName + '\", \"bookId\": ' + bookId+ ', \"year\": ' + year() + ', \"spread_ml\": ' + spreadMlAmount + ', \"amount\": ' + betAmount + ', \"betType\": \"' + betType + '\", \"timestamp\": \"' + (new Date()).toISOString() + '\"}';
        $.ajax({
            type: "POST",
            url: '/bets',
            contentType: "application/json",
            data: dataString,
            cache: false
        }).done(function(results) {
            statusUpdate(results, $('#bookId').val());
            displayCurrentBets();
        }).error(function(results) {
            errorUpdate(results, $('#bookId').val(), betType);
            displayCurrentBets();
        });
     };

    function statusColor(currentBet, strong) {
        var a = '1.0';
        if (!strong) a = '0.3';
        var bgcolor = 'rgba(255, 255, 255, 1.0)';
        if (currentBet.resultString == 'Not') bgcolor = 'rgba(255, 119, 119, ' + a + ')';
        else if (currentBet.resultString == 'Win') bgcolor = 'rgba(109, 255, 109, ' + a + ')';
        return bgcolor;
    };

    function displayCompetitors(e) {
        if ($('#' + e.currentTarget.id + ' span').hasClass('glyphicon-minus')) {
            $('[id^=competitors]').remove();
            $('[id^=button][id$=span]').removeClass('glyphicon-plus');
            $('[id^=button][id$=span]').removeClass('glyphicon-minus');
            $('[id^=button][id$=span]').addClass('glyphicon-plus');
        } else if (!queryingCompetitors) {
            $('[id^=competitors]').remove();
            $('[id^=button][id$=span]').removeClass('glyphicon-plus');
            $('[id^=button][id$=span]').removeClass('glyphicon-minus');
            $('[id^=button][id$=span]').addClass('glyphicon-plus');
            var span = $('#' + e.currentTarget.id + ' span');
            span.removeClass('glyphicon-plus');
            span.addClass('glyphicon-minus');
            var bet = e.currentTarget.id.replace('button', '');
            queryingCompetitors = true;
            $.ajax({
                type: 'GET',
                url: '/competition/' + year() + '/' + bet,
                cache: false
            }).done (function (bets) {
                var newTable = '';
                var userName = getCookie("WHOWON_USER");
                $.each(bets, function(key, currentBet) {
                    if (userName != currentBet.bet.userName) {
                        newTable = newTable +
                            '<tr id="competitors_' + bet + '_' + i + '">' +
                            '<td></td>' +
                            '<td>' + currentBet.bet.userName + '</td>' +
                            '<td>' + currentBet.bracket.teamName + '</td>' +
                            '<td>' + currentBet.bet.betType + '</td>' +
                            '<td>' + currentBet.bet.spread_ml + '</td>' +
                            '<td>$' + currentBet.bet.amount + '</td>' +
                            '<td style="background-color:' + statusColor(currentBet, false) + '">' + currentBet.resultString + '</td>' +
                            '</tr>';
                    }
                });
                $('[id^=competitors]').remove();
                $('#bets' + bet).after(newTable);
                queryingCompetitors = false;
            });
        }
    };

    function displayCurrentBets() {
        var userName = getCookie("WHOWON_USER")
		$.ajax({
			url: '/bets/' + userName + '/' + year(),
			cache: false
		}).done (function (bets) {
			$('tbody#bets_table_body').empty();
			var currentOutlay = 0.0;
			var currentWinnings = 0.0;
			$.each(bets, function(key, currentBet) {
			    currentOutlay += Number(currentBet.bet.amount);
			    currentWinnings += Number(currentBet.payoff);
				$('#bets_table_body').append(
                    '<tr id="bets' + currentBet.bet.bookId + '">' +
                    '<td><button class=\'btn btn-default btn-xs\' id=\'button' + currentBet.bet.bookId + '\'>' +
                    '<span id=\'button' + currentBet.bet.bookId + 'span\' class=\'glyphicon glyphicon-plus\'></span></button>' +
                    '<td>' + currentBet.bet.bookId + '</td>' +
					'<td>' + currentBet.bracket.teamName + '</td>' +
					'<td>' + currentBet.bet.betType + '</td>' +
					'<td>' + currentBet.bet.spread_ml + '</td>' +
					'<td>$' + currentBet.bet.amount + '</td>' +
					'<td style="background-color:' + statusColor(currentBet, true) + '">' + currentBet.resultString + '</td>' +
					'</tr>'
				);
				$('#button' + currentBet.bet.bookId).on('click', function (e) { displayCompetitors(e) });
			});
			$('#betHeader').text('(Outlay: $' + currentOutlay +', Wallet: $' + (currentWinnings - currentOutlay).toFixed(2) + ')');
            $('#runningQuery').addClass('hide');
		});
    };

    function formatTimestamp(timestamp) {
        var localTime = new Date(timestamp.getTime() - (timestamp.getTimezoneOffset() * 60000));
        var hours = localTime.getHours();
        var minutes = localTime.getMinutes();
        var ampm = " AM";
        if (hours > 12) {
           hours -= 12;
           ampm = " PM";
        }
        if (minutes < 10) minutes = "0" + minutes;
        return hours + ":" + minutes + ampm;
    };

    function winnerColor(currentGame, fav) {
        var bgcolor = '#FFFFFF';
        if (fav && currentGame.favScore > currentGame.undScore) bgcolor = '#FFD80D';
        if (!fav && currentGame.favScore < currentGame.undScore) bgcolor = '#FFD80D';
        return bgcolor;
    };

    function displayGameResults() {
		$.ajax({
			url: '/games/' + year(),
			cache: false
		}).done (function (games) {
			$('tbody#gamesLeft').empty();
			$('tbody#gamesRight').empty();
			for (i = 0; i < games.length; i = i + 2) {
			    var currentGame = games[i];
                var timestamp = formatTimestamp(new Date(currentGame.timestamp));
				$('#gamesLeft').append(
                    '<tr id="gameResult' + currentGame.favBookId + '">' +
					'<td>' + currentGame.favBookId + '</td>' +
					'<td>' + currentGame.favSeed + '</td>' +
					'<td>' + currentGame.favName + '</td>' +
					'<td>' + currentGame.favFirstHalfScore + '</td>' +
					'<td bgcolor="' + winnerColor(currentGame, true) + '">' + currentGame.favScore  + '</td>' +
					'<td>' + timestamp + '</td>' +
					'</tr>');
				var first = currentGame.favBookId;
				if (currentGame.undFirstTo15) {
				    first = currentGame.undBookId;
				}
				$('#gamesLeft').append(
                    '<tr id="gameResult' + currentGame.undBookId + '">' +
					'<td>' + currentGame.undBookId+ '</td>' +
					'<td>' + currentGame.undSeed + '</td>' +
					'<td>' + currentGame.undName + '</td>' +
					'<td>' + currentGame.undFirstHalfScore + '</td>' +
					'<td bgcolor="' + winnerColor(currentGame, false) + '">' + currentGame.undScore  + '</td>' +
					'<td>' + (currentGame.favScore + currentGame.undScore) + ' - ' + first + '</tr>' +
					'</tr><td/><td/><td/></tr>');
				if (i < games.length - 1) {
                    currentGame = games[i + 1];
                    timestamp = formatTimestamp(new Date(currentGame.timestamp));
                    $('#gamesRight').append(
                        '<tr id="gameResult' + currentGame.favBookId + '">' +
                        '<td>' + currentGame.favBookId + '</td>' +
                        '<td>' + currentGame.favSeed + '</td>' +
                        '<td>' + currentGame.favName + '</td>' +
    					'<td>' + currentGame.favFirstHalfScore + '</td>' +
                        '<td bgcolor="' + winnerColor(currentGame, true) + '">' + currentGame.favScore  + '</td>' +
                        '<td>' + timestamp + '</td>' +
                        '</tr>');
                    var first = currentGame.favBookId;
                    if (currentGame.undFirstTo15) {
                        first = currentGame.undBookId;
                    }
                    $('#gamesRight').append(
                        '<tr id="gameResult' + currentGame.undBookId + '">' +
                        '<td>' + currentGame.undBookId + '</td>' +
                        '<td>' + currentGame.undSeed + '</td>' +
                        '<td>' + currentGame.undName + '</td>' +
     					'<td>' + currentGame.undFirstHalfScore + '</td>' +
                        '<td bgcolor="' + winnerColor(currentGame, false) + '">' + currentGame.undScore  + '</td>' +
    					'<td>' + (currentGame.favScore + currentGame.undScore) + ' - ' + first + '</tr>' +
                        '</tr><td/><td/><td/></tr>');
                }
            }
    	});
    };

    function errorUpdate(results, bookId, betType) {
         if (results.responseText == 'Unknown Player') {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>Unknown Player</strong>');
         } else if (results.responseText == 'Unknown BookId') {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>Unknown BookId</strong>');
         }
    };

    function statusUpdate(results, bookId) {
        if (results.responseText == 'Bet Submitted') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>Submitted</strong>');
        } else if (results.responseText == 'Bet Replaced') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>Replaced previous</strong>');
        }
    };

    function populateYears() {
        $.ajax({
            url: '/years',
            cache: false
        }).done(function(results) {
			$('#years').empty();
			$.each(results, function(key, year_) {
			    if (results[0] == year_) {
    			    if (year == 0) {
                        $('#who-won-title').text('Who Won? ' + year())
                    } else {
                        $('#who-won-title').text('Who Won? All')
                    }
                    $('#years').append(
                        '<li value=\"' + year_ + '\" id=\"year_' + year_ + '\"><a href=\"#\">' + year_ + '</a></li>'
                    );
                    $('#yearDropdownLabel').html(year_ + "<span class=\"caret\"></span>");
			    } else {
                    $('#years').append(
                        '<li value=\"' + year_ + '\" id=\"year_' + year_ + '\"><a href=\"#\">' + year_ + '</a></li>'
                    );
                }
                $('#year_' + year_).click(function(event) {
                    $('#navbar').removeClass('in');
                    $.each(this.parentNode.childNodes, function (index, node) {
                        $('#' + node.id).removeClass('active');
                    });
                    $('#' + this.id).addClass('active');
                    $('#yearDropdownLabel').html(this.textContent + "<span class=\"caret\"></span>");
                    $('#who-won-title').text('Who Won? ' + year())
                    if (!$('#report').hasClass('hide')) {
                        updateWinnings();
                        displayCurrentBets();
                        displayGameResults();
                    } else {
                        displayCurrentBets();
                        displayGameResults();
                        populateBookIds();
                    }
                });
 			});
		});
    };

    function updateSpread(lower, upper, selected) {
    	$('#spreadAmount').empty();
		for (i = lower; i < upper; i = i + 0.5) {
		    var currentValue = '<option value =\"' + i + '\">' + i + '</option>';
            if (i == selected) currentValue = '<option value =\"' + i + '\" selected =\"selected\">' + i + '</option>'
			$('#spreadAmount').append(currentValue);
		}
	}

    function populateBookIds() {
        $.ajax({
            url: '/bookIds/' + year(),
            cache: false
        }).done(function(results) {
			$('#bookId').empty();
			$.each(results, function(key, currentGame) {
			    var labelString = currentGame.bookId + ' - ' + currentGame.teamName;
				$('#bookId').append(
					'<option value =\"' + labelString + '\">' + labelString + '</option>'
				);
                //if (currentGame.firstHalf != 0) {
	     		//    var labelString = currentGame.firstHalf + ' - ' + currentGame.teamName  + ' (1st Half)';
                //    $('#bookId').append(
                //        '<option value =\"' + labelString + '\">' + labelString + '</option>'
                //    );
			    //}
			    //if (currentGame.firstTo15 != 0) {
	     		//    var labelString = currentGame.firstTo15 + ' - ' + currentGame.teamName  + ' (1st to 15)';
                //    $('#bookId').append(
                //        '<option value =\"' + labelString + '\">' + labelString + '</option>'
                //    );
			    //}
 			});
		});
		updateSpread(-40.0, 40.0, 0.0);
    	$('#moneyline').empty();
		for (i = -2000.0; i <= 2000.0; i = i + 5) {
		    if (i <= -100.0 || i >= 100.0) {
     		    var currentValue = '<option value =\"' + i + '\">' + i + '</option>';
                if (i == 100.0) currentValue = '<option value =\"' + i + '\" selected =\"selected\">' + i + '</option>'
                $('#moneyline').append(currentValue);
		    }
		}
    	$('#betAmount').empty();
        $('#betAmount').append('<option value =\"1\">$1</option>');
		for (i = 5; i < 200; i = i + 5) {
			$('#betAmount').append(
				'<option value =\"' + i + '\">$' + i + '</option>'
			);
		}
    };

    function getCookie(cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for(var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0)==' ') c = c.substring(1);
            if (c.indexOf(name) == 0) return c.substring(name.length,c.length);
        }
        return "";
    };

    displayCurrentBets();
    displayGameResults();
    populateBookIds();
    populateYears();
});