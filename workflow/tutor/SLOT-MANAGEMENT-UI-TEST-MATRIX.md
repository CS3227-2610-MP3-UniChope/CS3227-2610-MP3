# Tutor Slot Management UI Test Matrix

| ID | Scenario | Expected result |
| --- | --- | --- |
| SMUI-01 | Load Tutor workspace | Calls modules and upcoming slots endpoints; renders owned slot fields and SGT times |
| SMUI-02 | Apply / clear start-date filter and refresh | React test confirms it sends only optional SGT `date`; all-upcoming reload omits date |
| SMUI-03 | Create with assigned module/date/start/end | Sends exact module ID and UTC `Z` instants; clears time fields and reloads slots on success |
| SMUI-04 | Missing fields or end not after start | No POST; show validation; preserve input |
| SMUI-05 | Create receives API failure | Show safe error; preserve module/date/time values; no automatic retry |
| SMUI-06 | Cancel AVAILABLE slot and reject confirmation | No POST when rejected; confirmed action sends `{}` and reloads slots |
| SMUI-07 | BOOKED slot | No cancellation control is rendered |
| SMUI-08 | Slot operations while booking/note state is loaded | Slot filters and refresh do not change booking filters or selected note |
