/*******************************************************************************
 * Copyright (c) 2013-2014 eBay Software Foundation
 *
 * See the file license.txt for copying permission.
 ******************************************************************************/
package org.reactivesource.util;

import org.testng.annotations.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.reactivesource.testing.TestConstants.SMALL;
import static org.reactivesource.util.Assert.*;


public class AssertTest {
    @Test(groups = SMALL)
    public void testIsTrueDoesNotThrowExceptionForTrueStatement() {
        isTrue(true, "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testIsTrueThrowsIllegalArgumentExpressionForFalseStatement() {
        isTrue(false, "");
    }

    @Test(groups = SMALL)
    public void testNotNullDoesNotThrowExceptionForNotNullValue() {
        notNull(new Object(), "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testNotNullThrowsExceptionForNullValue() {
        notNull(null, "");
    }

    @Test(groups = SMALL)
    public void testHasTextDoesNotThrowExceptionForNotCorrectValue() {
        hasText("asdasd", "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testHasTextThrowsExceptionForIncorrectValue() {
        hasText("", "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testHasTextThrowsExceptionForNullValue() {
        hasText(null, "");
    }

    @Test(groups = SMALL)
    public void testStateDoesNotThrowExceptionForTrueStatement() {
        state(true, "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalStateException.class)
    public void testStateThrowsIllegalStateExpressionForFalseStatement() {
        state(false, "");
    }

    @Test(groups = SMALL)
    public void testNotEmptyDoesNotThrowExceptionForListWithElements() {
        notEmpty(newArrayList(1), "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testNotEmptyThrowsExceptionForNullList() {
        notEmpty(null, "");
    }

    @Test(groups = SMALL, expectedExceptions = IllegalArgumentException.class)
    public void testNotEmptyThrowsExceptionForEmptyList() {
        notEmpty(newArrayList(), "");
    }
}
