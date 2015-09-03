create temporary table if not exists x
(index idx_membersip_uuid (individual_uuid))
select
	m.individual_uuid,
	l.districtName as district,
	l.communityName as community,
	case gh.firstName when 'null' then null else gh.firstName end as firstName,
	case gh.middleName when 'null' then null else gh.middleName end as middleName,
	case gh.lastName when 'null' then null else gh.lastName end as lastName
from
	membership m
join
	socialgroup sg on m.socialGroup_uuid = sg.uuid
join
	individual gh on sg.groupHead_uuid = gh.uuid
join
	location l on sg.location_uuid = l.uuid