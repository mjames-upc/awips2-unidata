/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package edu.ucar.unidata.uf.viz.aqi;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import edu.ucar.unidata.common.dataplugin.aqi.AQIRecord;

/**
 * Abstract AQI Renderable object
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 7, 2014            mschenke     Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class AQIRenderable implements IRenderable {

    private final IMapDescriptor descriptor;

    private AQIRecord record;

    private double[] recordLocation;

    private RGB color = new RGB(126, 126, 126);

    private ColorMapParameters parameters;

    private HorizontalAlignment horizontalTextAlignment = HorizontalAlignment.CENTER;

    private VerticalAlignment verticalTextAlignment = VerticalAlignment.MIDDLE;

    private IFont font;

    public AQIRenderable(IMapDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public AQIRecord getRecord() {
        return record;
    }

    public void setRecord(AQIRecord record) {
        this.record = record;
        if (record != null) {
            Coordinate location = record.getGeometry().getCoordinate();
            this.recordLocation = descriptor.worldToPixel(new double[] {
                    location.x, location.y });
        } else {
            this.recordLocation = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.drawables.IRenderable#paint(com.raytheon.uf.
     * viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        DrawableString string = getAQIString(paintProps);
        if (string != null) {
            target.drawStrings(string);
        }
    }

    public DrawableString getAQIString(PaintProperties paintProps) {
        DrawableString string = null;
        if (recordLocation != null) {
            string = new DrawableString(Integer.toString(record.getAqi()),
                    getColorByValue(record.getAqi()));
            string.setCoordinates(recordLocation[0], recordLocation[1]);
            string.horizontalAlignment = getHorizontalTextAlignment();
            string.verticalAlignment = getVerticalTextAlignment();
            string.basics.alpha = paintProps.getAlpha();
            string.font = getFont();
        }
        return string;
    }

    public RGB getColorByValue(float value) {
        if (parameters != null) {
            Color color = parameters.getColorByValue(value);
            if (color != null) {
                return new RGB((int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));
            }
        }
        return getColor();
    }

    public RGB getColor() {
        return color;
    }

    public void setColor(RGB color) {
        this.color = color;
    }

    public ColorMapParameters getParameters() {
        return parameters;
    }

    public void setParameters(ColorMapParameters parameters) {
        this.parameters = parameters;
    }

    public HorizontalAlignment getHorizontalTextAlignment() {
        return horizontalTextAlignment;
    }

    public void setHorizontalTextAlignment(
            HorizontalAlignment horizontalTextAlignment) {
        this.horizontalTextAlignment = horizontalTextAlignment;
    }

    public VerticalAlignment getVerticalTextAlignment() {
        return verticalTextAlignment;
    }

    public void setVerticalTextAlignment(VerticalAlignment verticalTextAlignment) {
        this.verticalTextAlignment = verticalTextAlignment;
    }

    public IFont getFont() {
        return font;
    }

    public void setFont(IFont font) {
        this.font = font;
    }

    public double[] getRecordLocation() {
        return recordLocation;
    }

}