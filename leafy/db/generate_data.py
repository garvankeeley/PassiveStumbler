import json
import pg8000
import sys
import geopy, math
from geopy.distance import vincenty

def create_cell_geo(lat, lon):
    d = vincenty((0, 0), (lat, 0)).km
    y = math.floor(d * 2) / 2
    d = vincenty((lat, 0), (lat, lon)).km
    x = math.floor(d * 2) / 2
    dest1 = vincenty(kilometers=y).destination((0, 0), 0)
    dest2 = vincenty(kilometers=x).destination((lat, 0), 90)
    lat, lon = dest1.latitude, dest2.longitude
    
    result = []
    kDist = 0.5
    origin = geopy.Point(lat, lon);
    dest = vincenty(kilometers=kDist).destination(origin, 0)
    lat2, lon2 = dest.latitude, dest.longitude
    dest = vincenty(kilometers=kDist).destination(dest, 90)
    lat3, lon3 = dest.latitude, dest.longitude
    dest = vincenty(kilometers=kDist).destination(origin, 90)
    lat4, lon4 = dest.latitude, dest.longitude
    result = [(lat, lon), (lat2, lon2), (lat3, lon3), (lat4, lon4), (lat, lon)]
    result = [[round(a, 8) for a in x] for x in result]
    return result

#if __name__ == '__main__':

_cell_was_created = False

def get_or_create_cell(coord):
    #print 'coord', coord
    conn = pg8000.connect()
    curs = conn.cursor()
    pk = None
    try:
        curs.execute("SELECT gridcell_pk FROM gridcell where " +
                     "ST_Contains(wkb_geometry, ST_GeomFromText('POINT(%.8f %.8f)',4326))" %
                     (coord[0], coord[1]))
        pk = curs.fetchone()[0]
    except:
        pass

    global _cell_was_created
    if pk:
        _cell_was_created = False
        return pk

    lat = coord[1]
    lon = coord[0]
    if not isinstance(lat, float) or not isinstance(lon, float):
        print "bad lat lon: ", lat, lon
        return None

    geo = create_cell_geo(lat, lon)
    geo_string = 'POLYGON((' + ','.join([' '.join([str(a) for a in x]) for x in geo]) + '))'

    conn = pg8000.connect()
    curs = conn.cursor()
    curs.execute("insert into gridcell(wkb_geometry) values(ST_GeomFromText('%s', 4326))"
                 " returning gridcell_pk" % geo_string)
    conn.commit()
    _cell_was_created = True
    return curs.fetchone()[0]

def create_random_users():
    conn = pg8000.connect()
    curs = conn.cursor()
    for i in range(1900, 2100):
        try:
            curs.execute("INSERT INTO userinfo(name) VALUES ('%s')" % ('name' + str(i)))
            conn.comit()
        except:
            print sys.exc_info()[0]
            conn.rollback()
            conn = pg8000.connect()
            curs = conn.cursor()

def get_user_pk(username):
    conn = pg8000.connect()
    curs = conn.cursor()
    pk = None
    try:
        curs.execute("select userinfo_pk from userinfo where name='%s'" % username)
        pk = curs.fetchone()[0]
    except:
        pass
    return pk

def insert_week(week, year, obs, userpk, cellpk):
    conn = pg8000.connect()
    curs = conn.cursor()
    try:
        curs.execute("""
            insert into week(week_num, year, observations, userinfo_fk, gridcell_fk)
             values(%d, %d, %d, %d, %d)
        """ % (week, year, obs, userpk, cellpk))
        conn.commit()
    finally:
        pass

from random import randint
def func(coords):
    for coord in coords:
        cell = get_or_create_cell(coord)
        if not cell:
            continue
        for i in range(0, 5):
            user = get_user_pk('name' + str(randint(0, 1000)))
            #print cell, user
            try:
              insert_week(10, 2015, randint(10,100), user, cell)
            except pg8000.core.ProgrammingError as ex:
                print ex

def doit():
    with open('world.geo.json') as data_file:
        data = json.load(data_file)
        for item in data['features']:
            for c in item['geometry']['coordinates']:
                func(c)


cc = [-79.4, 43.7]



#create_random_users()

