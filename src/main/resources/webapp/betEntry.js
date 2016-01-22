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

    $('#betSubmit').click(function() {
        submitBet();
    });

    function submitBet() {
        $('#runningQuery').removeClass('hide');
        var bookId = $('#bookId').val().split(' ')[0];
        var spreadMlAmount = '0';
        var betAmount = $('#betAmount').val();
        var userName = getCookie("WHOWON_USER")
        loadingTimestamp = new Date();
        var betType = '';
        if ($('moneylinePane').hasClass('hide')) {
            betType = 'ST';
            spreadMlAmount = $('#spreadAmount').val();
        } else {
            betType = 'ML';
            spreadMlAmount = $('#moneyline').val();
        }
        var dataString = '{\"userName\": \"' + userName + '\", \"bookId\": ' + bookId+ ', \"year\": ' + loadingTimestamp.getFullYear() + ', \"spread_ml\": ' + spreadMlAmount + ', \"amount\": ' + betAmount + ', \"betType\": \"' + betType + '\"}';
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
            errorUpdate(results, $('#bookId').val());
            displayCurrentBets();
        });
     };

    function displayCurrentBets() {
        var userName = getCookie("WHOWON_USER")
        loadingTimestamp = new Date();
		$.ajax({
			url: '/bets/' + userName + '/' + loadingTimestamp.getFullYear(),
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
			$('#betHeader').text('(Outlay: $' + currentOutlay +', Winnings: $' + currentWinnings + ')');
            $('#runningQuery').addClass('hide');
		});
    };

    function errorUpdate(results, bookId) {
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
            $('#submitResult').html('<strong>   Submitted </strong> for '+ bookId);
         } else if (results.responseText == 'Bet Replaced') {
            $('#submitResult').addClass('alert-success');
            $('#submitResult').removeClass('alert-danger');
            $('#submitResult').html('<strong>   Replaced previous bet </strong> for '+ bookId);
         }
    };

    function statusUpdate(results, bookId) {
        $('#runningQuery').addClass('hide');
    };

    function populateBookIds() {
        loadingTimestamp = new Date();
        $.ajax({
            url: '/bookIds/' + '2015',
            cache: false
        }).done(function(results) {
			$('#bookId').empty();
			$.each(results, function(key, currentGame) {
			    var labelString = currentGame.bookId + ' - ' + currentGame.teamName;
				$('#bookId').append(
					'<option value =\"' + labelString + '\">' + labelString + '</option>'
				);
 			});
            $('#runningQuery').addClass('hide');
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
    populateBookIds();
});