import { useEffect, useState } from 'react'
import { GeoJSON, MapContainer, TileLayer } from 'react-leaflet'
import type { Feature, FeatureCollection, Geometry } from 'geojson'
import type { Layer, PathOptions } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { FundingByRecipient } from '../api/reports'

const usd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

// Sequential blue choropleth scale. Each bucket is a lower bound in US$.
const SCALE = [
  { min: 15e9, color: '#08306b', label: '≥ $15B' },
  { min: 10e9, color: '#08519c', label: '$10–15B' },
  { min: 5e9, color: '#2171b5', label: '$5–10B' },
  { min: 1e9, color: '#4292c6', label: '$1–5B' },
  { min: 0, color: '#9ecae1', label: '< $1B' },
]
const NO_DATA = '#eef0f2'

function colorFor(amount: number | undefined): string {
  if (amount === undefined) return NO_DATA
  for (const bucket of SCALE) {
    if (amount >= bucket.min) return bucket.color
  }
  return NO_DATA
}

export function FundingMap({ rows }: { rows: FundingByRecipient[] }) {
  const [geo, setGeo] = useState<FeatureCollection | null>(null)
  const [geoError, setGeoError] = useState(false)

  // The country boundaries are served as a static file from /public.
  useEffect(() => {
    fetch('/countries.geo.json')
      .then((r) => r.json())
      .then(setGeo)
      .catch(() => setGeoError(true))
  }, [])

  // Index the funding rows by ISO3 so each map feature can find its total in O(1).
  const byIso = new Map<string, FundingByRecipient>()
  for (const r of rows) {
    if (r.isoCode) byIso.set(r.isoCode.toUpperCase(), r)
  }

  if (geoError) return <p className="status error">Could not load the map boundaries.</p>
  if (!geo) return <p className="status">Loading map…</p>

  const styleFeature = (feature?: Feature<Geometry>): PathOptions => {
    const iso = feature?.id as string | undefined
    const row = iso ? byIso.get(iso.toUpperCase()) : undefined
    return { fillColor: colorFor(row?.totalAmount), fillOpacity: 0.85, color: '#ffffff', weight: 1 }
  }

  const bindTooltip = (feature: Feature<Geometry>, layer: Layer) => {
    const iso = feature.id as string | undefined
    const row = iso ? byIso.get(iso.toUpperCase()) : undefined
    const name = (feature.properties as { name?: string } | null)?.name ?? iso ?? 'Unknown'
    layer.bindTooltip(
      row ? `${name}: ${usd.format(row.totalAmount)} (${row.flowCount} flows)` : `${name}: no data`,
      { sticky: true },
    )
  }

  return (
    <div className="map-wrap">
      <MapContainer center={[2, 20]} zoom={3} scrollWheelZoom={false} style={{ height: 460, width: '100%' }}>
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {/* keying on byIso.size forces the layer to restyle when data arrives */}
        <GeoJSON key={byIso.size} data={geo} style={styleFeature} onEachFeature={bindTooltip} />
      </MapContainer>
      <div className="legend">
        {SCALE.map((b) => (
          <span key={b.label} className="legend-item">
            <span className="swatch" style={{ background: b.color }} />
            {b.label}
          </span>
        ))}
        <span className="legend-item">
          <span className="swatch" style={{ background: NO_DATA }} />
          no data
        </span>
      </div>
    </div>
  )
}
