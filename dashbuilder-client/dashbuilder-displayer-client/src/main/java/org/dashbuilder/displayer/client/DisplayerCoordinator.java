/**
 * Copyright (C) 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dashbuilder.displayer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Callback;
import org.dashbuilder.common.client.error.ClientRuntimeError;
import org.dashbuilder.dataset.group.DataSetGroup;

/**
 * The coordinator class holds a list of Displayer instances and it makes sure that the data shared among
 * all of them is properly synced. This means every time a data display modification request comes from any
 * of the displayer components the rest are updated to reflect those changes.
 */
public class DisplayerCoordinator {

    protected List<Displayer> displayerList = new ArrayList<Displayer>();
    protected Map<RendererLibrary,List<Displayer>> rendererMap = new HashMap<RendererLibrary,List<Displayer>>();
    protected CoordinatorListener displayerListener = new CoordinatorListener();

    public void addDisplayer(Displayer displayer) {
        if (displayer == null) return;

        displayerList.add(displayer);
        displayer.addListener(displayerListener);

        RendererLibrary renderer = RendererManager.get().getRendererForDisplayer(displayer.getDisplayerSettings());
        List<Displayer> rendererGroup = rendererMap.get(renderer);
        if (rendererGroup == null) rendererMap.put(renderer, rendererGroup = new ArrayList<Displayer>());
        rendererGroup.add(displayer);
    }

    public List<Displayer> getDisplayerList() {
        return displayerList;
    }

    public boolean removeDisplayer(Displayer displayer) {
        if (displayer == null) return false;
        RendererLibrary renderer = RendererManager.get().getRendererForDisplayer(displayer.getDisplayerSettings());
        List<Displayer> rendererGroup = rendererMap.get(renderer);
        if (rendererGroup != null) rendererGroup.remove(displayer);

        return displayerList.remove(displayer);
    }

    public void drawAll() {
        drawAll(null);
    }

    public void redrawAll() {
        redrawAll(null);
    }

    public void drawAll(Callback callback) {
        displayerListener.init(callback, displayerList.size(), true);
        for (RendererLibrary renderer : rendererMap.keySet()) {
            List<Displayer> rendererGroup = rendererMap.get(renderer);
            renderer.draw(rendererGroup);
        }
    }

    public void redrawAll(Callback callback) {
        displayerListener.init(callback, displayerList.size(), false);
        for (RendererLibrary renderer : rendererMap.keySet()) {
            List<Displayer> rendererGroup = rendererMap.get(renderer);
            renderer.redraw(rendererGroup);
        }
    }

    public void closeAll() {
        for (Displayer displayer : displayerList) {
            displayer.close();
        }
    }

    /**
     * Internal class that listens to events raised by any of the Displayer instances handled by this coordinator.
     */
    private class CoordinatorListener implements DisplayerListener {

        int count = 0;
        int total = 0;
        Callback callback;
        boolean draw;

        protected void init(Callback callback, int total, boolean draw) {
            count = 0;
            this.callback= callback;
            this.draw = draw;
            this.total = total;
        }

        protected void count() {
            count++;
            if (count == total && callback != null) {
                callback.onSuccess(null);
            }
        }

        public void onDraw(Displayer displayer) {
            if (draw) count();
            for (Displayer other : displayerList) {
                if (other == displayer) continue;
                other.onDraw(displayer);
            }
        }

        public void onRedraw(Displayer displayer) {
            if (!draw) count();
            for (Displayer other : displayerList) {
                if (other == displayer) continue;
                other.onRedraw(displayer);
            }
        }

        public void onClose(Displayer displayer) {
            for (Displayer other : displayerList) {
                if (other == displayer) continue;
                other.onClose(displayer);
            }
        }

        public void onFilterEnabled(Displayer displayer, DataSetGroup groupOp) {
            for (Displayer other : displayerList) {
                if (other == displayer) continue;
                other.onFilterEnabled(displayer, groupOp);
            }
        }

        public void onFilterReset(Displayer displayer, List<DataSetGroup> groupOps) {
            for (Displayer other : displayerList) {
                if (other == displayer) continue;
                other.onFilterReset(displayer, groupOps);
            }
        }

        @Override
        public void onError(final Displayer displayer, ClientRuntimeError error) {
            for (Displayer other : displayerList) {
                if (other == displayer) continue;
                other.onError(displayer, error);
            }
        }
    }
}