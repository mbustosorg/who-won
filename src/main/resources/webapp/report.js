/*

    Copyright (C) 2015 Mauricio Bustos (m@bustos.org)

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

    var currentExpanded = '';

    function updateRiderEvents(e) {
        var bibNumber = e.currentTarget.id.replace('button', '');
        $.ajax({
            type: "GET",
            url: '/rider/' + bibNumber + '/events',
            cache: false
        }).done (function (riderEvents) {
            var lowerBound = 1;
            if (riderEvents.length == 1) lowerBound = 0;
            var newTable = '';
            for (i = lowerBound; i < riderEvents.length; i++) {
                newTable = newTable +
                    '<tr id="toggle' + bibNumber + '_' + i + '">' +
                    '<td></td>' +
                    '<td></td>' +
                    '<td></td>' +
                    '<td>' + riderEvents[i].timestamp + '</td>' +
                    '<td>' + riderEvents[i].stop + '</td>' +
                    '</tr>';
            }
            $('#events' + bibNumber).after(newTable);

        });
        if (currentExpanded != '') $('[id^=' + currentExpanded + ']').hide();
        currentExpanded = 'toggle' + bibNumber;
    };

    function handleReportSelect() {
        $.ajax({
            type: "GET",
            url: '/restStopCounts',
            cache: false
        }).done(function(results) {
            chartData(results)
        });
		$.ajax({
			url: '/riderStatus',
			cache: false
		}).done (function (riderStatus) {
			$('tbody#riderstatus_table_body').empty();
			$.each(riderStatus, function(key, currentStatus) {
				$('#riderstatus_table_body').append(
                    '<tr id="events' + currentStatus.bibNumber + '">' +
                    '<td><button class=\'btn btn-default btn-xs\' id=\'button' + currentStatus.bibNumber + '\'>' +
                    '<span class=\'glyphicon glyphicon-plus-sign\'></span></button>' +
                    '</td>' +
					'<td>' + currentStatus.bibNumber + '</td>' +
					'<td>' + currentStatus.name + '</td>' +
					'<td>' + currentStatus.timestamp + '</td>' +
					'<td>' + currentStatus.stop + '</td>' +
					'</tr>'
				);
				$('#button' + currentStatus.bibNumber).on('click', function (e) { updateRiderEvents(e) });
			});
		});
        setTimeout(handleReportSelect, 60000);
    };

    function chartData(results) {
      var dataResults = [['Stop', 'Count']];
      for (i = 0; i < results.length; i++) {
         dataResults[i + 1] = [results[i][0], results[i][1]];
      }
      var data = google.visualization.arrayToDataTable(dataResults);

      var options = {
        title: 'Rider count by last rest stop',
        hAxis: {
          title: 'Stop'
        },
        vAxis: {
          title: 'Count',
          minValue: 0
        }
      };
      var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
      chart.draw(data, options);
    };

    handleReportSelect();
});