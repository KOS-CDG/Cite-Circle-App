package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * The two scopes a shared element needs, published so components can opt in without every screen
 * between the NavHost and the component growing two parameters.
 *
 * Threading `SharedTransitionScope` and `AnimatedContentScope` down by hand is viral: reaching the
 * avatar in a person row means changing DiscoverScreen, PeopleScreen and PersonRow, none of which
 * have any interest in transitions. CompositionLocals keep the opt-in at the two call sites that
 * actually share something.
 *
 * Both default to null, and [com.example.ui.components.Avatar] falls back to no shared element
 * when either is missing -- so an avatar rendered outside a NavHost (a screenshot test, a preview)
 * still composes.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedScope = compositionLocalOf<AnimatedContentScope?> { null }

/**
 * Key for the avatar shared between a person row and that person's profile. Centralised because
 * the two ends must agree exactly, and a silently mismatched key produces no transition and no
 * error -- the failure mode is "nothing happens", which is hard to tell from "not implemented".
 */
fun avatarSharedKey(userId: String): String = "avatar-$userId"
