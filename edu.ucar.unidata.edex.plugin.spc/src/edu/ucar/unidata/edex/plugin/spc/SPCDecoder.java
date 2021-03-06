package edu.ucar.unidata.edex.plugin.spc;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.vividsolutions.jts.geom.GeometryFactory;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;
import edu.ucar.unidata.common.dataplugin.spc.SPCRecord;

/**
 * 
 * Decoder for SPC convective outlook KML files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Engineer    Description
 * ------------ ----------- --------------------------
 * Mar 27, 2018 mjames      Initial creation
 * Mar 29, 2018 mjames      Smoothing by coordinate sampling
 * 
 * </pre>
 *
 * @author mjames@ucar.edu
 */

public class SPCDecoder {

	GeometryFactory geomFact = new GeometryFactory();

	private IUFStatusHandler logger = UFStatus.getHandler(SPCDecoder.class);    

	private Map<String, String> reportType = SPCRecord.mapDefinitions(report, reportRegex);

	private static final String[] report = { 
			"Convective Outlook", 
			"Tornado Outlook",
			"Wind Outlook",
			"Hail Outlook"
			//,"Thunderstorm Outlook"
	};
	private static final String[] reportRegex = { 
			"cat", 
			"torn", 
			"wind",
			"hail"
			//,"enh"
	};
	
	public PluginDataObject[] decode(byte[] data) throws Exception {

		ArrayList<SPCRecord> list = new ArrayList<SPCRecord>();

		// Modification needed for unmarshaling
		String input = new String(data).replace("xmlns=\"http://earth.google.com/kml/2.2\"", 
				"xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\"" );

		ByteArrayInputStream stream = new ByteArrayInputStream( input.getBytes( "UTF-8" ) );
		
		try {
			Document document = (Document) Kml.unmarshal(stream).getFeature();
			List<Feature> folders = document.getFeature();
	
			for(Feature feature : folders) {
				
				String folderName = feature.getName();
				String type = null;
				for (Entry<String, String> typ : reportType.entrySet()) {
					if (folderName.endsWith(typ.getValue())) {
						type = typ.getKey();
						break;
					}
				}

				if (feature instanceof Folder) {
					
					Folder folder = (Folder) feature;
					List<Feature> placemarkList = folder.getFeature();
					int reportCount = 0;
					
					for (Feature mark : placemarkList ) {
						reportCount++;
						Placemark placemark = (Placemark) mark;
						String category = placemark.getName();
						Geometry geometry = placemark.getGeometry();
						
						if(geometry instanceof Polygon) {
							
							Polygon polygon = (Polygon) geometry;
							
							TimeSpan timePrimitive = (TimeSpan) placemark.getTimePrimitive();
							String dateString = timePrimitive.getBegin();
							Date date = new DateTime(dateString).toDate();
							DataTime dataTime = new DataTime(date);
							
							try {
								SPCRecord record = new SPCRecord();
								record.setDataTime(dataTime);
								record.setTypeCategory(category);
								record.setReportType(type);
								record.setReportPart(reportCount);
								record.setGeometry(geomFact.createPolygon(
										modifyLinearRing(polygon.getOuterBoundaryIs().getLinearRing())));
								list.add(record);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
					logger.info(placemarkList.size() + " polygons processed for KML feature " + folderName.toString() );
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		PluginDataObject[] decodedData = list.toArray(new PluginDataObject[list.size()]);
		return (decodedData);
	}

	public com.vividsolutions.jts.geom.LinearRing modifyLinearRing(LinearRing l) {
		return geomFact.createLinearRing(this.convertCoords(l.getCoordinates()));
	}
	
	/**
	 * 
	 * Convert Coordinate list from KML to JTS
	 * 
	 * @param geomList
	 * @return
	 */
	public com.vividsolutions.jts.geom.Coordinate[] convertCoords(List<Coordinate> geomList) {

		if (1 == 0 && geomList.size() > 40) {
			
			// Downsample if large DISABLED
			int inc = 4;
			int limit = Math.round(geomList.size()/inc);
			com.vividsolutions.jts.geom.Coordinate[] coordList = new com.vividsolutions.jts.geom.Coordinate[limit];
			for (int i = 0; i < coordList.length; i++) {
				com.vividsolutions.jts.geom.Coordinate b = new com.vividsolutions.jts.geom.Coordinate();
				b.x = geomList.get(i*inc).getLongitude();
				b.y = geomList.get(i*inc).getLatitude();
				coordList[i] = b;
			}
			// Ensure last coordinates are equal for a valid closed LinearRing
			coordList[coordList.length-1].x = geomList.get(geomList.size()-1).getLongitude();
			coordList[coordList.length-1].y = geomList.get(geomList.size()-1).getLatitude();
			return coordList;
			
		} else {
			
			int j = 0;
			com.vividsolutions.jts.geom.Coordinate[] coordList = new com.vividsolutions.jts.geom.Coordinate[geomList.size()];
			for (Coordinate i : geomList) {
				com.vividsolutions.jts.geom.Coordinate b = new com.vividsolutions.jts.geom.Coordinate();
				b.x = i.getLongitude();
				b.y = i.getLatitude();
				coordList[j] = b;
				j = j + 1;
			}
			return coordList;
			
		}
	}
}
