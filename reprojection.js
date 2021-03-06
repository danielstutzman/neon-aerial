if (window.devicePixelRatio != 1.0) {
  window.alert('Browser devicePixelRatio is ' + window.devicePixelRatio +
    '; zoom to 100% for best looking results and map that doesn\'t twitch');
}

proj4.defs('EPSG:32613', '+proj=utm +zone=13 +ellps=WGS84 +datum=WGS84 ' +
    '+units=m +no_defs');
proj4.defs('EPSG:32616', '+proj=utm +zone=16 +ellps=WGS84 +datum=WGS84 ' +
    '+units=m +no_defs');
proj4.defs('EPSG:32617', '+proj=utm +zone=17 +ellps=WGS84 +datum=WGS84 ' +
    '+units=m +no_defs');
proj4.defs('EPSG:32618', '+proj=utm +zone=18 +ellps=WGS84 +datum=WGS84 ' +
    '+units=m +no_defs');
proj4.defs('EPSG:32619', '+proj=utm +zone=19 +ellps=WGS84 +datum=WGS84 ' +
    '+units=m +no_defs');

var DOMAIN_NUM_TO_COLOR = {
   1: '#46b569',  2: '#13372b',  3: '#a1ba3a',  4: '#ffa23b',  5: '#46256c',
   6: '#477b4a',  7: '#477b4a',  8: '#b5c489',  9: '#597978', 10: '#7d6e37',
  11: '#72963e', 12: '#72963e', 13: '#244357', 14: '#b4763b', 15: '#1d5d5f',
  16: '#5d83a8', 17: '#97895c', 18: '#7a8e99', 19: '#7a8e99', 20: '#f2b01e',
};

var layers = {};

layers['osm'] = new ol.layer.Tile({
  source: new ol.source.OSM()
});

layers['labels'] = new ol.layer.Vector({
  source: new ol.source.Vector({
    features: []
  }),
  style: function(feature) {
    return new ol.style.Style({
      image: new ol.style.Circle({
        radius: 20,
        stroke: new ol.style.Stroke({ color: '#fff' }),
        fill: new ol.style.Fill({ color: '#3399CC' })
      }),
      text: new ol.style.Text({
        text: feature.get('name'),
        fill: new ol.style.Fill({ color: '#fff' })
      })
    });
  },
  minResolution: 1000
});

layers['domains'] = new ol.layer.Vector({
  source: new ol.source.Vector({
    url: 'output/neon_domains.geojson',
    format: new ol.format.GeoJSON()
  }),
  style: function(feature, resolution) {
    return new ol.style.Style({
      fill: new ol.style.Fill({
        color: DOMAIN_NUM_TO_COLOR[feature.get('DomainID')]
      }),
      text: new ol.style.Text({
        text: 'D' + feature.get('DomainID').toString(),
        fill: new ol.style.Fill({ color: 'rgba(255,255,255,0.2)' }),
        font: 'bold 20pt sans-serif',
      })
    });
  }
});

layers['states'] = new ol.layer.Vector({
  source: new ol.source.Vector({
    url: 'output/usa_states.geojson',
    format: new ol.format.GeoJSON()
  }),
  style: function(feature, resolution) {
    return new ol.style.Style({
      stroke: new ol.style.Stroke({ color: 'rgba(0,0,0,0.2)', width: 0.5 })
    });
  }
});

var map = new ol.Map({
  controls:
    ol.control.defaults({
      attributionOptions: { collapsible: false }
    }).extend([ new ol.control.ScaleLine() ]),
  pixelRatio: 1, // keep map from twitching
  layers: [
    layers['osm'],
    //layers['osm'],
    layers['domains'],
    layers['labels'],
    layers['states']
  ],
  target: 'map',
  view: new ol.View({
    projection: 'EPSG:3857',
    center: ol.proj.fromLonLat([ -99, 38 ]),
    zoom: 3.7
  })
});

//var graticule = new ol.Graticule();
//graticule.setMap(map);

function addLayer() {
  map.addLayer(layers['combine_l3_camera']);
}
function removeLayer() {
  map.removeLayer(layers['combine_l3_camera']);
}

var xhr = new XMLHttpRequest();
xhr.onreadystatechange = function() {
  if (xhr.readyState == 4 && xhr.status == 200) {
    var newLayers = JSON.parse(xhr.responseText);
    for (var i = 0; i < newLayers.length; i++) {
      var newLayer = newLayers[i];
      map.addLayer(new ol.layer.Image({
        opacity: 1.0,
        source: new ol.source.ImageStatic({
          crossOrigin: '',
          projection: newLayer.projection,
          url: 'output/combine_l3_camera/' + newLayer.siteName + '.png',
          imageExtent: [
            newLayer.minEasting,
            newLayer.minNorthing,
            newLayer.maxEasting,
            newLayer.maxNorthing
          ]
        })
      }));
      layers['labels'].getSource().addFeature(new ol.Feature({
        geometry: new ol.geom.Point(ol.proj.fromLonLat(
          proj4(newLayer.projection, 'WGS84').forward(
            [ newLayer.minEasting, newLayer.minNorthing ]))),
        name: newLayer.siteName
      }));
    }
  }
};
xhr.open('GET', 'output/layers.json', true);
xhr.send();
