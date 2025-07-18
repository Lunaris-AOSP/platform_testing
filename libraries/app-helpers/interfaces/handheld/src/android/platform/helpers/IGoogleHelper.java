/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.helpers;

import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;

public interface IGoogleHelper extends IAppHelper {

    public enum NavigationTab {
        DISCOVER("Discover"),
        SNAPSHOT("Snapshot"),
        SEARCH("Search"),
        COLLECTIONS("Collections"),
        MORE("More");

        private final String text;

        NavigationTab(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    };

    public enum SearchResultTab {
        ALL("All"),
        IMAGES("Images"),
        VIDEOS("Videos"),
        NEWS("News"),
        MAPS("Maps"),
        BOOKS("Books"),
        SHOPPING("Shopping"),
        FLIGHTS("Flights");

        private final String mDisplayName;

        SearchResultTab(String displayName) {
            mDisplayName = displayName;
        }

        @Override
        public String toString() {
            return mDisplayName;
        }
    }

    /**
     * Setup expectations: Google app open
     *
     * <p>This method will start a voice search
     *
     * @return true if the search is started, false otherwise
     */
    public boolean doVoiceSearch();

    /**
     * Setup expectations: Google app open to a search result
     *
     * <p>This method will return the query from the search
     */
    public String getSearchQuery();

    /**
     * Setup expectations: Google app open.
     *
     * <p>This method goes to the specified tab, such as Discover, Search, Collections.
     */
    public void navigateToTab(NavigationTab tab);

    /**
     * Setup expectations: In home and with search bar.
     *
     * <p>This method starts search from the search bar in home.
     */
    public default void startSearch() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Google app open.
     *
     * <p>This method inputs keyword in search box.
     *
     * @param query The keyword to search.
     */
    public default void inputSearch(String query) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Google app open and a search keyword input.
     *
     * <p>This method clicks the search button and waits for the results.
     */
    public default void clickSearchButtonAndWaitForResults() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Google app open to a search result.
     *
     * <p>This method flings the search results.
     *
     * @param dir The direction of the fling, must be UP or DOWN.
     */
    public default void flingSearchResults(Direction dir) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Google app open to a search result.
     *
     * <p>This method scroll the search results.
     *
     * @param dir The direction of the fling, must be UP or DOWN.
     */
    public default void scrollSearchResults(Direction dir) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: In home.
     *
     * <p>This method flings right to Google Feed.
     */
    public default void openFeed() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Feed open.
     *
     * <p>This method flings the feed.
     *
     * @param dir The direction of the fling, must be UP or DOWN.
     */
    public default void flingFeed(Direction dir) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Feed open.
     *
     * <p>Get the UiObject2 of feed container.
     */
    public UiObject2 getFeedContainer();

    /**
     * Setup expectations: Feed open.
     *
     * <p>This method flings the feed.
     *
     * @param container The container with scrollable elements.
     * @param dir The direction of the fling, must be UP or DOWN.
     */
    public void flingFeed(UiObject2 container, Direction dir);

    /**
     * Setup expectations: Feed open.
     *
     * <p>This method scrolls the feed with speed.
     *
     * @param container The container with scrollable elements.
     * @param dir The direction of the scroll, must be UP or DOWN.
     * @param speed The speed of scroll.
     */
    public void scrollFeed(UiObject2 container, Direction dir, int speed);

    /**
     * Setup expectations: Google app open and has done a search.
     *
     * <p>This method will go to the specified search result tab.
     *
     * @param tab one of the tabs of the search result page.
     */
    public abstract void openSearchResultTab(IGoogleHelper.SearchResultTab tab);

    /**
     * Setup expectations: In home.
     *
     * <p>This method clear search result.
     */
    public default boolean clearSearchResult() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Feed open.
     *
     * <p>This method disable the autoplay video preivews
     */
    void disableAutoplayVideoPreviews();
}
