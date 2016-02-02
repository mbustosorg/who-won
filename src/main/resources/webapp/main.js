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
    var loadingTimestamp = 0;

	var video = document.getElementById("snapVideo"),
		videoObj = { "video": true },
		errBack = function(error) {
			console.log("Video capture error: ", error.code);
		};

	// Put video listeners into place
	if(navigator.getUserMedia) { // Standard
		navigator.getUserMedia(videoObj, function(stream) {
			video.src = stream;
			video.play();
		}, errBack);
	} else if(navigator.webkitGetUserMedia) { // WebKit-prefixed
		navigator.webkitGetUserMedia(videoObj, function(stream){
			video.src = window.URL.createObjectURL(stream);
			video.play();
		}, errBack);
	}
	else if(navigator.mozGetUserMedia) { // Firefox-prefixed
		navigator.mozGetUserMedia(videoObj, function(stream){
			video.src = window.URL.createObjectURL(stream);
			video.play();
		}, errBack);
	}
    $('#snapButton').click(function() {
        $('#snapVideo').addClass('hide');
        var newCanvas = document.createElement("canvas");
        newCanvas.width = video.videoWidth;
        newCanvas.height = video.videoHeight;
        newCanvas.getContext('2d').drawImage(video, 0, 0, newCanvas.width, newCanvas.height);
        var img = new Image();
        img.src = newCanvas.toDataURL();
        img.id = 'snapImage';
        $('#photoPage').prepend(img);
        Caman("#snapImage", function () {
          this.greyscale();
          this.render();
        });

//        var img = document.createElement("img");
//        img.src = canvas.toDataURL();
 //       $('#snapCanvas').append(img);
//        $('#photoPage').append(
//            '<img id="testSnapImage" src="' + canvas.toDataURL() + '"></img>');

        //        var canvas = document.getElementById("snapCanvas");
//        var dataURL = canvas.toDataURL("image/png");
//        $.ajax({
//            type: "POST",
//            url: '/ticket/',
//            dataType: 'json',
//            data: dataURL
//        }).done(function(results) {
//        }).error(function(results) {
//        });
    });
    $('#snapRetakeButton').click(function() {
        $('#snapImage').remove();
        $('#snapVideo').removeClass('hide');
    });

    $('#straightBetNav').click(function() {
        $('#manualEntryTab').removeClass('hide');
        $('#photoPage').addClass('hide');
    });
    $('#moneyLineBetNav').click(function() {
        $('#manualEntryTab').removeClass('hide');
        $('#photoPage').addClass('hide');
    });
    $('#scanBetNav').click(function() {
        $('#manualEntryTab').addClass('hide');
        $('#photoPage').removeClass('hide');
    });

    $('#betSubmit').click(function() {
        submitBet();
    });
    $('#entryPageNav').click(function() {
        $('#betEntry').removeClass('hide');
        $('#report').addClass('hide');
        $('#entryPageNav').addClass('active');
        $('#reportPageNav').removeClass('active');
    });
    $('#reportPageNav').click(function() {
        $('#report').removeClass('hide');
        $('#betEntry').addClass('hide');
        $('#reportPageNav').addClass('active');
        $('#entryPageNav').removeClass('active');
        updateWinnings();
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
				$('#bets_table_body').append(
                    '<tr id="bets' + currentBet.bet.bookId + '">' +
					'<td>' + currentBet.bet.bookId + '</td>' +
					'<td>' + currentBet.bracket.teamName + '</td>' +
					'<td>' + currentBet.bet.betType + '</td>' +
					'<td>' + currentBet.bet.spread_ml + '</td>' +
					'<td>$' + currentBet.bet.amount + '</td>' +
					'<td>' + currentBet.resultString + '</td>' +
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
        if (hours > 12) hours -= 12;
        if (minutes < 10) minutes = "0" + minutes;
        return hours + ":" + minutes;
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
				$('#gamesLeft').append(
                    '<tr id="gameResult' + currentGame.favBookId + '">' +
					'<td>' + currentGame.favBookId + '</td>' +
					'<td>' + currentGame.favSeed + '</td>' +
					'<td>' + currentGame.favName + '</td>' +
					'<td>' + currentGame.favScore  + '</td>' +
					'<td>' + timestamp + '</td>' +
					'</tr>');
				$('#gamesLeft').append(
                    '<tr id="gameResult' + currentGame.undBookId + '">' +
					'<td>' + currentGame.undBookId+ '</td>' +
					'<td>' + currentGame.undSeed + '</td>' +
					'<td>' + currentGame.undName + '</td>' +
					'<td>' + currentGame.undScore  + '</td>' +
					'</tr><tr><td/><td/><td/><td/></tr>');
				if (i < games.length - 1) {
                    currentGame = games[i + 1];
                    timestamp = formatTimestamp(new Date(currentGame.timestamp));
                    $('#gamesRight').append(
                        '<tr id="gameResult' + currentGame.favBookId + '">' +
                        '<td>' + currentGame.favBookId + '</td>' +
                        '<td>' + currentGame.favSeed + '</td>' +
                        '<td>' + currentGame.favName + '</td>' +
                        '<td>' + currentGame.favScore  + '</td>' +
                        '<td>' + timestamp + '</td>' +
                        '</tr>');
                    $('#gamesRight').append(
                        '<tr id="gameResult' + currentGame.undBookId + '">' +
                        '<td>' + currentGame.undBookId + '</td>' +
                        '<td>' + currentGame.undSeed + '</td>' +
                        '<td>' + currentGame.undName + '</td>' +
                        '<td>' + currentGame.undScore  + '</td>' +
                        '</tr><tr><td/><td/><td/><td/></tr>');
                }
            }
    	});
    };

    function errorUpdate(results, bookId, betType) {
         if (results.responseText == 'Unknown Player') {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>   Error: </strong> Unknown Player');
         } else if (results.responseText == 'Unknown BookId') {
            $('#submitResult').addClass('alert-danger');
            $('#submitResult').removeClass('alert-success');
            $('#submitResult').html('<strong>   Error: </strong> Unknown BookId ' + bookId);
         } else if (results.responseText == 'Bet Submitted') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>   Submitted ' + betType + ' bet</strong> for '+ bookId);
         } else if (results.responseText == 'Bet Replaced') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>   Replaced previous ' + betType + ' bet </strong> for '+ bookId);
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