# Admin and authentication reuse record

Scope: Kok Seng backend increment, based on team main98ca801. This is not a reuse
measurement for teammates' contributions or the whole application.

| Source | Treatment |
| --- | --- |
| MP2 AdminService/AdminQueries | Management rules and count/rate definitions informed specifications; no source file or method body copied. |
| MP2 AuthenticationService/Argon2PasswordHasher | Account-flow and hashing choices informed specifications; web/session code newly written against MP3 interfaces. |
| MP3 AccountStorage/core repositories/lifecycle | Existing teammate APIs and implementations reused by dependency; not counted as newly authored source. No core file modified. |
| MP3 core/auth/Tutor review SQL | Runtime V1/V2/V3 preserve schema SQL in a distinct active sequence. Respectively42,3,12 nonblank SQL lines reused from team files after removing review-only comments. No claim these are original new schemas. |
| Runtime V4 bootstrap marker | Four new nonblank SQL lines. |
| Existing shared security configuration | Extended for auth/Admin profiles and current-account checks; retained closed default and Tutor-only test profile compatibility. |

## Count method

Count nonblank lines excluding lines starting with Java comment markers in the new
Admin/auth/runtime main and test Java files. Result: **2911 lines in 31 files**.
This is a simple source-size measure, not an AST statement count. Against MP2 source
within this scope:0 lines imported unchanged,0 source lines directly edited from
imported MP2 code,2911 newly written lines (0percent/0percent/100percent). Behavioral
ideas/rules adapted from MP2 are acknowledged above rather than misreported as source
line reuse. Source was generated with AI assistance and reviewed/tested.

The count excludes existing shared core/Tutor source, modified shared security code,
the new current-account web filter, SQL, configuration, build files and documentation;
these integration changes are identified separately above. It does not assign shared
code to Kok Seng or claim project-wide percentages. Recalculate if this scope changes.
