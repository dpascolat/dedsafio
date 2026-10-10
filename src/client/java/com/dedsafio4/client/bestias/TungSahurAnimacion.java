package com.dedsafio4.client.bestias;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Las animaciones del Tung Tung Tung Sahur (del mod italian_brainrots_ft, de luisd): idle, walk, attack y attack_tung.
 */
public final class TungSahurAnimacion {
   private TungSahurAnimacion() {}

   public static final AnimationDefinition idle = AnimationDefinition.Builder.withLength(4.0F)
      .looping()
      .addAnimation(
         "left_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "bone19",
         new AnimationChannel(AnimationChannel.Targets.SCALE, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)})
      )
      .addAnimation(
         "bone20",
         new AnimationChannel(AnimationChannel.Targets.SCALE, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)})
      )
      .addAnimation(
         "raig",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -357.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, -717.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, -1075.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, -1435.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "raig",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.4F, 1.4F, 1.4F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.5F, KeyframeAnimations.scaleVec(1.4F, 1.4F, 1.4F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(2.5F, KeyframeAnimations.scaleVec(1.4F, 1.4F, 1.4F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(3.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(3.5F, KeyframeAnimations.scaleVec(1.4F, 1.4F, 1.4F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(4.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.6F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, -0.6F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(3.0F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(4.0F, KeyframeAnimations.posVec(0.0F, -0.6F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition walk = AnimationDefinition.Builder.withLength(1.1667F)
      .looping()
      .addAnimation(
         "bone",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 1.2F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F, -5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.degreeVec(0.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.0833F, KeyframeAnimations.posVec(0.0F, -2.14F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, -1.14F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "head",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "left_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, -7.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.1001F, 14.3829F, 2.1995F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, -7.5F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(39.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 5.0F, 7.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.degreeVec(39.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "left_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.1667F, KeyframeAnimations.degreeVec(23.0144F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.4167F, KeyframeAnimations.degreeVec(-18.0394F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6667F, KeyframeAnimations.degreeVec(-10.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.degreeVec(10.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "left_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 2.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F, 0.5F, 0.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_leg",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6667F, KeyframeAnimations.degreeVec(10.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.8333F, KeyframeAnimations.degreeVec(23.0144F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.degreeVec(-10.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6667F, KeyframeAnimations.posVec(0.0F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 2.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition attack = AnimationDefinition.Builder.withLength(0.5F)
      .addAnimation(
         "right_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.0833F, KeyframeAnimations.degreeVec(-153.7675F, 16.6658F, -25.3112F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-153.77F, 16.67F, -25.31F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.degreeVec(-57.863F, -0.168F, -36.258F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.degreeVec(-57.86F, -0.17F, -36.26F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_arm",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.0833F, KeyframeAnimations.posVec(0.0F, 0.9F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, 0.9F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.posVec(0.1F, -0.6F, -0.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.1F, -0.6F, -0.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "bone19",
         new AnimationChannel(AnimationChannel.Targets.SCALE, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)})
      )
      .addAnimation(
         "bone20",
         new AnimationChannel(AnimationChannel.Targets.SCALE, new Keyframe[]{new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.0, 0.0, 0.0), AnimationChannel.Interpolations.LINEAR)})
      )
      .addAnimation(
         "raig",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -357.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "raig",
         new AnimationChannel(
            AnimationChannel.Targets.SCALE,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.scaleVec(1.4F, 1.4F, 1.4F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0, 1.0, 1.0), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.125F, KeyframeAnimations.degreeVec(-7.5F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-7.5F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.25F, KeyframeAnimations.degreeVec(22.4657F, -7.3934F, 1.6325F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.2917F, KeyframeAnimations.degreeVec(19.9657F, -7.3934F, 1.6325F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.degreeVec(19.97F, -7.39F, 1.63F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .build();
   public static final AnimationDefinition attack_tung = AnimationDefinition.Builder.withLength(0.6F)
      .addAnimation(
         "root",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -17.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 17.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -17.5F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "chest",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "left_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, 27.5F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "left_arm",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(1.0F, -1.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, -2.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.posVec(1.0F, -1.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_arm",
         new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.degreeVec(-180.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_arm",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(-1.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, -2.0F, -3.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.posVec(-1.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "left_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(1.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.posVec(1.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .addAnimation(
         "right_leg",
         new AnimationChannel(
            AnimationChannel.Targets.POSITION,
            new Keyframe[]{
               new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.3F, KeyframeAnimations.posVec(-1.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
               new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            }
         )
      )
      .build();
}
