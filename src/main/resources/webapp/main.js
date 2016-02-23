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

    var loadingTimestamp = 0;

	var video = document.getElementById("snapVideo");
    var videoObj = { video: true };
    var videoAudioObj = { video: true, audio: true };
    var webcamStream = null;

    function startVideo() {
        if (webcamStream == null) {
            errBack = function(error) {
                console.log("Video capture error: ", error.code);
            };
            if(navigator.getUserMedia) { // Standard
                navigator.getUserMedia(videoObj, function(stream) {
                    webcamStream = stream;
                    video.src = stream;
                    video.play();
                }, errBack);
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

    $('#snapButton').click(function() {
        $('video').addClass('hide');
        stopVideo();
        var newCanvas = document.createElement("canvas");
        newCanvas.width = video.videoWidth;
        newCanvas.height = video.videoHeight;
        newCanvas.getContext('2d').drawImage(video, 0, 0, newCanvas.width, newCanvas.height);
        var img = new Image();
        img.src = newCanvas.toDataURL();
        img.id = 'snapImage';
        $('#photoPage').prepend(img);
        $('#snapImage').css('width','300');
    });

    $('#snapRetakeButton').click(function() {
        $('#snapImage').remove();
        $('video').removeClass('hide');
        startVideo();
    });

    $('#mobileInputPhoto').change(function () {
      if (this.files && this.files[0]) {
        var reader = new FileReader();
        reader.onload = function (e) {
            if ($('#snapImage') != null) $('#snapImage').remove();
            var img = new Image();
            img.src = e.target.result;
            img.id = 'snapImage';
            $('#photoPage').prepend(img);
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

    $('#betSubmit').click(function() {
        if ($('#photoPage').hasClass('hide')) submitBet();
        else {
            $.ajax({
                type: "POST",
                url: '/ticket/',
                dataType: 'json',
                data: $('#snapImage').attr('src')
            }).done(function(results) {
            }).error(function(results) {
            });
        }
    });
    $('#entryPageNav').click(function() {
        $('#betEntry').removeClass('hide');
        $('#report').addClass('hide');
        $('#admin').addClass('hide');
        $('#entryPageNav').addClass('active');
        $('#reportPageNav').removeClass('active');
        $('#adminPageNav').removeClass('active');
    });
    $('#reportPageNav').click(function() {
        $('#betEntry').addClass('hide');
        $('#report').removeClass('hide');
        $('#admin').addClass('hide');
        $('#entryPageNav').removeClass('active');
        $('#reportPageNav').addClass('active');
        $('#adminPageNav').removeClass('active');
        updateWinnings();
    });
    $('#adminPageNav').click(function() {
        $('#report').addClass('hide');
        $('#betEntry').addClass('hide');
        $('#admin').removeClass('hide');
        $('#reportPageNav').removeClass('active');
        $('#entryPageNav').removeClass('active');
        $('#adminPageNav').addClass('active');
    });

    function updateWinnings() {
        if (!$('#report').hasClass('hide')) {
            $('#reportQuery').removeClass('hide');
            var year = loadingTimestamp.getFullYear();
            year = 2015;
            $.ajax({
                url: '/winnings/' + year,
                cache: false
            }).done(function(results) {
                var data = new google.visualization.DataTable();
                data.addColumn('date', 'Time');
                var pctData = new google.visualization.DataTable();
                pctData.addColumn('date', 'Time');
                for (i = 0; i < results.list.length; i++) {
                  data.addColumn('number', results.list[i].userName);
                  pctData.addColumn('number', results.list[i].userName);
                }
                for (i = 0; i < results.timestamps.length; i++) {
                  var row = [new Date(results.timestamps[i])];
                  var pctRow = [new Date(results.timestamps[i])];
                  for (j = 0; j < results.list.length; j++) {
                    row[j + 1] = Number(results.list[j].winnings[i].toFixed(2));
                    pctRow[j + 1] = Number(results.list[j].percentage[i].toFixed(2));
                  }
                  data.addRow(row);
                  pctData.addRow(pctRow);
                }
                var options = {
                  title: 'Net Winnings',
                  legend: { position: 'bottom' },
                  chartArea: { left: 60, top: 30, width: '85%', height: '80%'},
                  hAxis: {
                    title: 'Time',
                    gridlines: {
                      count: -1,
                      units: {
                        days: {format: ['MMM dd']},
                        hours: {format: ['HH:mm', 'ha']},
                      }
                    },
                    minorGridlines: {
                      units: {
                        hours: {format: ['hh:mm:ss a', 'ha']},
                        minutes: {format: ['HH:mm a Z', ':mm']}
                      }
                    }
                  },
                  vAxis: {
                    title: '$',
                    minValue: 0
                  }
                };
                var chart = new google.visualization.LineChart(document.getElementById('winningsChart'));
                chart.draw(data, options);
                var options = {
                  title: 'Percentage Won',
                  legend: { position: 'bottom' },
                  chartArea: { left: 60, top: 30, width: '85%', height: '80%'},
                  hAxis: {
                    title: 'Time',
                    gridlines: {
                      count: -1,
                      units: {
                        days: {format: ['MMM dd']},
                        hours: {format: ['HH:mm', 'ha']},
                      }
                    },
                    minorGridlines: {
                      units: {
                        hours: {format: ['hh:mm:ss a', 'ha']},
                        minutes: {format: ['HH:mm a Z', ':mm']}
                      }
                    }
                  },
                  vAxis: {
                    title: '%',
                    minValue: 0
                  }
                };
                var chart = new google.visualization.LineChart(document.getElementById('percentageWinChart'));
                chart.draw(pctData, options);
                $('#reportQuery').addClass('hide');
            });
            setTimeout(updateWinnings, 60000);
        }
    };

    function submitBet() {
        $('#runningQuery').removeClass('hide');
        var bookId = $('#bookId').val().split(' ')[0];
        var spreadMlAmount = '0';
        var betAmount = $('#betAmount').val();
        var userName = getCookie("WHOWON_USER")
        loadingTimestamp = new Date();
        var year = loadingTimestamp.getFullYear();
        year = 2015
        var betType = '';
        if ($('#moneylinePane').hasClass('active')) {
            betType = 'ML';
            spreadMlAmount = $('#moneyline').val();
        } else {
            betType = 'ST';
            spreadMlAmount = $('#spreadAmount').val();
        }
        var dataString = '{\"userName\": \"' + userName + '\", \"bookId\": ' + bookId+ ', \"year\": ' + year + ', \"spread_ml\": ' + spreadMlAmount + ', \"amount\": ' + betAmount + ', \"betType\": \"' + betType + '\"}';
        $.ajax({
            type: "POST",
            url: '/bets/',
            dataType: "json",
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

    function displayCurrentBets() {
        var userName = getCookie("WHOWON_USER")
        loadingTimestamp = new Date();
        var year = loadingTimestamp.getFullYear();
        year = 2015;
		$.ajax({
			url: '/bets/' + userName + '/' + year,
			cache: false
		}).done (function (bets) {
			$('tbody#bets_table_body').empty();
			var currentOutlay = 0.0;
			var currentWinnings = 0.0;
			$.each(bets, function(key, currentBet) {
			    currentOutlay += Number(currentBet.bet.amount);
			    currentWinnings += Number(currentBet.payoff);
                var bgcolor = '#FFFFFF';
                if (currentBet.resultString == 'Lose') bgcolor = '#FF0017';
                else if (currentBet.resultString == 'Win') bgcolor = '#6DFF6C';
				$('#bets_table_body').append(
                    '<tr id="bets' + currentBet.bet.bookId + '">' +
					'<td>' + currentBet.bet.bookId + '</td>' +
					'<td>' + currentBet.bracket.teamName + '</td>' +
					'<td>' + currentBet.bet.betType + '</td>' +
					'<td>' + currentBet.bet.spread_ml + '</td>' +
					'<td>$' + currentBet.bet.amount + '</td>' +
					'<td bgcolor="' + bgcolor + '">' + currentBet.resultString + '</td>' +
					'</tr>'
				);
			});
			$('#betHeader').text('(Outlay: $' + currentOutlay +', Winnings: $' + currentWinnings.toFixed(2) + ')');
            $('#runningQuery').addClass('hide');
		});
    };

    function formatTimestamp(timestamp) {
        var hours = timestamp.getHours();
        var minutes = timestamp.getMinutes();
        var ampm = " AM";
        if (hours > 12) {
           hours -= 12;
           ampm = " PM";
        }
        if (minutes < 10) minutes = "0" + minutes;
        return hours + ":" + minutes + ampm;
    };

    function displayGameResults() {
        loadingTimestamp = new Date();
        var year = loadingTimestamp.getFullYear();
        year = 2015;
		$.ajax({
			url: '/games/' + year,
			cache: false
		}).done (function (games) {
			$('tbody#gamesLeft').empty();
			$('tbody#gamesRight').empty();
			for (i = 0; i < games.length; i = i + 2) {
			    var currentGame = games[i];
                var timestamp = formatTimestamp(new Date(currentGame.timestamp));
                var bgcolor = '#FFFFFF';
                if (currentGame.favScore > currentGame.undScore) bgcolor = '#FFD80D';
				$('#gamesLeft').append(
                    '<tr id="gameResult' + currentGame.favBookId + '">' +
					'<td>' + currentGame.favBookId + '</td>' +
					'<td>' + currentGame.favSeed + '</td>' +
					'<td>' + currentGame.favName + '</td>' +
					'<td bgcolor="' + bgcolor + '">' + currentGame.favScore  + '</td>' +
					'<td>' + timestamp + '</td>' +
					'</tr>');
                bgcolor = '#FFFFFF';
                if (currentGame.favScore < currentGame.undScore) bgcolor = '#FFD80D';
				$('#gamesLeft').append(
                    '<tr id="gameResult' + currentGame.undBookId + '">' +
					'<td>' + currentGame.undBookId+ '</td>' +
					'<td>' + currentGame.undSeed + '</td>' +
					'<td>' + currentGame.undName + '</td>' +
					'<td bgcolor="' + bgcolor + '">' + currentGame.undScore  + '</td>' +
					'</tr><tr><td/><td/><td/><td/></tr>');
				if (i < games.length - 1) {
                    currentGame = games[i + 1];
                    timestamp = formatTimestamp(new Date(currentGame.timestamp));
                    bgcolor = '#FFFFFF';
                    if (currentGame.favScore > currentGame.undScore) bgcolor = '#FFD80D';
                    $('#gamesRight').append(
                        '<tr id="gameResult' + currentGame.favBookId + '">' +
                        '<td>' + currentGame.favBookId + '</td>' +
                        '<td>' + currentGame.favSeed + '</td>' +
                        '<td>' + currentGame.favName + '</td>' +
                        '<td bgcolor="' + bgcolor + '">' + currentGame.favScore  + '</td>' +
                        '<td>' + timestamp + '</td>' +
                        '</tr>');
                    bgcolor = '#FFFFFF';
                    if (currentGame.favScore < currentGame.undScore) bgcolor = '#FFD80D';
                    $('#gamesRight').append(
                        '<tr id="gameResult' + currentGame.undBookId + '">' +
                        '<td>' + currentGame.undBookId + '</td>' +
                        '<td>' + currentGame.undSeed + '</td>' +
                        '<td>' + currentGame.undName + '</td>' +
                        '<td bgcolor="' + bgcolor + '">' + currentGame.undScore  + '</td>' +
                        '</tr><tr><td/><td/><td/><td/></tr>');
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
         } else if (results.responseText == 'Bet Submitted') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>Submitted</strong>');
         } else if (results.responseText == 'Bet Replaced') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>Replaced previous</strong>');
         }
    };

    function statusUpdate(results, bookId) {
        $('#runningQuery').addClass('hide');
    };

    function populateBookIds() {
        loadingTimestamp = new Date();
        var year = loadingTimestamp.getFullYear();
        year = 2015;
        $.ajax({
            url: '/bookIds/' + year,
            cache: false
        }).done(function(results) {
			$('#bookId').empty();
			$.each(results, function(key, currentGame) {
			    var labelString = currentGame.bookId + ' - ' + currentGame.teamName;
				$('#bookId').append(
					'<option value =\"' + labelString + '\">' + labelString + '</option>'
				);
 			});
		});
    	$('#spreadAmount').empty();
		for (i = -40.0; i < 40; i = i + 0.5) {
		    var currentValue = '<option value =\"' + i + '\">' + i + '</option>';
            if (i == 0) currentValue = '<option value =\"' + i + '\" selected =\"selected\">' + i + '</option>'
			$('#spreadAmount').append(currentValue);
		}
    	$('#moneyline').empty();
		for (i = -400.0; i <= 400; i = i + 10) {
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
});