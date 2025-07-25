package com.trademart;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.trademart.search.SearchController;
import com.trademart.search.SearchItem;

public class SearchTest {
//
//     SearchController controller = new SearchController();
//
//     @Test
//     public void test_extractKeyword(){
//         SearchController searchController = new SearchController();
//         String query = "Big 12Blue Balls1234";
//
//         ArrayList<String> terms = searchController.extractTerms(query);
//         List<String> expecteds= List.of("Big", "12", "Blue", "Balls", "1234");
//
//         assertEquals(expecteds, terms);
//     }
//
//     @Test
//     public void test_extractKeyword2(){
//         SearchController searchController = new SearchController();
//         String query = "John Doe@!1234";
//
//         ArrayList<String> terms = searchController.extractTerms(query);
//         List<String> expecteds= List.of("John", "Doe", "@", "!", "1234");
//
//         assertEquals(expecteds, terms);
//     }
//
//     // @Test
//     // public void test_filter(){
//     //     ArrayList<SearchItem> items = new ArrayList<>();
//     //     items.add(new SearchItem(0, "Lawn Mowing Job at Pasay"));
//     //     items.add(new SearchItem(0, "Lawn Trimming"));
//     //     items.add(new SearchItem(0, "Brother"));
//     //     items.add(new SearchItem(0, "Mowing Job at makati"));
//     //     String query = "Lawn Mowing Job";
//     //
//     //     ArrayList<String> expecteds = new ArrayList<>();
//     //     expecteds.add("Lawn Mowing Job at Pasay");
//     //     expecteds.add("Lawn Trimming");
//     //     expecteds.add("Mowing Job at makati");
//     //
//     //     ArrayList<SearchItem> filteredItems = controller.filter(query, items);
//     //     ArrayList<String> actuals = new ArrayList<>();
//     //     actuals.add(filteredItems.get(0).getTerm());
//     //     actuals.add(filteredItems.get(1).getTerm());
//     //     actuals.add(filteredItems.get(2).getTerm());
//     //
//     //     assertEquals(expecteds, actuals);
//     //
//     // }
//
//     @Test
//     public void test_filter(){
//         ArrayList<SearchItem> items = new ArrayList<>();
//         items.add(new SearchItem(0, "Programming Job"));
//         items.add(new SearchItem(0, "Lawn Mowing Job at Pasay"));
//         items.add(new SearchItem(0, "Lawn Trimming"));
//         items.add(new SearchItem(0, "Brother"));
//         items.add(new SearchItem(0, "Mowing Job at makati"));
//
//         String query = "Lawn Mowing Job";
//
//         ArrayList<String> expectedTitles = new ArrayList<>();
//         expectedTitles.add("Lawn Mowing Job at Pasay");
//         expectedTitles.add("Mowing Job at makati");
//         expectedTitles.add("Lawn Trimming");
//         expectedTitles.add("Programming Job");
//
//         ArrayList<Double> expectedRels = new ArrayList<>();
//         expectedRels.add(3.0);
//         expectedRels.add(2.0);
//         expectedRels.add(1.0);
//         expectedRels.add(1.0);
//
//         ArrayList<SearchItem> filteredItems = controller.filter(query, items);
//
//         ArrayList<String> actualTitles = new ArrayList<>();
//         actualTitles.add(filteredItems.get(0).getTerm());
//         actualTitles.add(filteredItems.get(1).getTerm());
//         actualTitles.add(filteredItems.get(2).getTerm());
//         actualTitles.add(filteredItems.get(3).getTerm());
//
//         ArrayList<Double> actualRels = new ArrayList<>();
//         actualRels.add(filteredItems.get(0).getRelPoints());
//         actualRels.add(filteredItems.get(1).getRelPoints());
//         actualRels.add(filteredItems.get(2).getRelPoints());
//         actualRels.add(filteredItems.get(3).getRelPoints());
//
//         assertEquals(expectedRels, actualRels);
//         assertEquals(expectedTitles, actualTitles);
//     }
//
}
