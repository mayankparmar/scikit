package scikit.graphics.dim3;
import java.lang.Math;

/**
 * A 4-element quaternion represented by double precision floating 
 * point x,y,z,w coordinates.  The quaternion is always normalized.
 *
 * This code derives from javax.vecmath.Quat4d
 */

public class Quaternion {
	final static double EPS = 0.000001;
	final static double EPS2 = 1.0e-30;
	final static double PIO2 = 1.57079632679;
	double x, y, z, w;

	/**
	 * Constructs and initializes a Quaternion representing the unit transformation
	 */
	public Quaternion() {
		this(0, 0, 0, 1);
	}
	
	/**
	 * Constructs and initializes a Quaternion from the specified xyzw coordinates.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param w the w scalar component
	 */
	public Quaternion(double x, double y, double z, double w)
	{
		double mag;
		mag = 1.0/Math.sqrt( x*x + y*y + z*z + w*w );
		this.x =  x*mag;
		this.y =  y*mag;
		this.z =  z*mag;
		this.w =  w*mag;

	}

	/**
	 * Constructs and initializes a Quaternion from the array of length 4. 
	 * @param q the array of length 4 containing xyzw in order
	 */
	public Quaternion(double[] q)
	{
		double mag; 
		mag = 1.0/Math.sqrt( q[0]*q[0] + q[1]*q[1] + q[2]*q[2] + q[3]*q[3] );
		x =  q[0]*mag;
		y =  q[1]*mag;
		z =  q[2]*mag;
		w =  q[3]*mag;
	}

	/**
	 * Constructs and initializes a Quaternion from the specified Quaternion.
	 * @param q1 the Quaternion containing the initialization x y z w data
	 */
	public Quaternion(Quaternion q1)
	{
		x = q1.x;
		y = q1.y;
		z = q1.z;
		w = q1.w;
	}

	/**
	 * Sets the value of this quaternion to the conjugate of quaternion q1.
	 * @param q1 the source vector
	 */
	public final void conjugate(Quaternion q1)
	{
		this.x = -q1.x;
		this.y = -q1.y;
		this.z = -q1.z;
		this.w = q1.w;
	}


	/**
	 * Negate the value of of each of this quaternion's x,y,z coordinates 
	 *  in place.
	 */
	public final void conjugate()
	{
		this.x = -this.x;
		this.y = -this.y;
		this.z = -this.z;
	}


	/**
	 * Sets the value of this quaternion to the quaternion product of
	 * quaternions q1 and q2 (this = q1 * q2).  
	 * Note that this is safe for aliasing (e.g. this can be q1 or q2).
	 * @param q1 the first quaternion
	 * @param q2 the second quaternion
	 */
	public final void mul(Quaternion q1, Quaternion q2)
	{
		if (this != q1 && this != q2) {
			this.w = q1.w*q2.w - q1.x*q2.x - q1.y*q2.y - q1.z*q2.z;
			this.x = q1.w*q2.x + q2.w*q1.x + q1.y*q2.z - q1.z*q2.y;
			this.y = q1.w*q2.y + q2.w*q1.y - q1.x*q2.z + q1.z*q2.x;
			this.z = q1.w*q2.z + q2.w*q1.z + q1.x*q2.y - q1.y*q2.x;
		} else {
			double	x, y, w;

			w = q1.w*q2.w - q1.x*q2.x - q1.y*q2.y - q1.z*q2.z;
			x = q1.w*q2.x + q2.w*q1.x + q1.y*q2.z - q1.z*q2.y;
			y = q1.w*q2.y + q2.w*q1.y - q1.x*q2.z + q1.z*q2.x;
			this.z = q1.w*q2.z + q2.w*q1.z + q1.x*q2.y - q1.y*q2.x;
			this.w = w;
			this.x = x;
			this.y = y;
		}
	}


	/**
	 * Sets the value of this quaternion to the quaternion product of
	 * itself and q1 (this = this * q1).  
	 * @param q1 the other quaternion
	 */
	public final void mul(Quaternion q1)
	{
		double x, y, w; 
		w = this.w*q1.w - this.x*q1.x - this.y*q1.y - this.z*q1.z;
		x = this.w*q1.x + q1.w*this.x + this.y*q1.z - this.z*q1.y;
		y = this.w*q1.y + q1.w*this.y - this.x*q1.z + this.z*q1.x;
		this.z = this.w*q1.z + q1.w*this.z + this.x*q1.y - this.y*q1.x;
		this.w = w;
		this.x = x;
		this.y = y;
	} 


	/** 
	 * Multiplies quaternion q1 by the inverse of quaternion q2 and places
	 * the value into this quaternion.  The value of both argument quaternions 
	 * is preservered (this = q1 * q2^-1).
	 * @param q1 the first quaternion 
	 * @param q2 the second quaternion
	 */ 
	public final void mulInverse(Quaternion q1, Quaternion q2) 
	{   
		Quaternion tempQuat = new Quaternion(q2);  
		tempQuat.inverse(); 
		this.mul(q1, tempQuat); 
	}


	/**
	 * Multiplies this quaternion by the inverse of quaternion q1 and places
	 * the value into this quaternion.  The value of the argument quaternion
	 * is preserved (this = this * q^-1).
	 * @param q1 the other quaternion
	 */
	public final void mulInverse(Quaternion q1)
	{  
		Quaternion tempQuat = new Quaternion(q1);
		tempQuat.inverse();
		this.mul(tempQuat);
	}


	/**
	 * Sets the value of this quaternion to quaternion inverse of quaternion q1.
	 * @param q1 the quaternion to be inverted
	 */
	public final void inverse(Quaternion q1)
	{
		double norm;

		norm = 1.0/(q1.w*q1.w + q1.x*q1.x + q1.y*q1.y + q1.z*q1.z);
		this.w =  norm*q1.w;
		this.x = -norm*q1.x;
		this.y = -norm*q1.y;
		this.z = -norm*q1.z;
	}


	/**
	 * Sets the value of this quaternion to the quaternion inverse of itself.
	 */
	public final void inverse()
	{
		double norm;  

		norm = 1.0/(this.w*this.w + this.x*this.x + this.y*this.y + this.z*this.z);
		this.w *=  norm;
		this.x *= -norm;
		this.y *= -norm;
		this.z *= -norm;
	}

	
	public final double norm() {
		return Math.sqrt(this.w*this.w + this.x*this.x + this.y*this.y + this.z*this.z);
	}
	
	
	/**
	 * Sets the value of this quaternion to the normalized value
	 * of quaternion q1.
	 * @param q1 the quaternion to be normalized.
	 */
	public final void normalize(Quaternion q1)
	{
		double norm;

		norm = (q1.x*q1.x + q1.y*q1.y + q1.z*q1.z + q1.w*q1.w);
		if (norm == 0)
			throw new IllegalStateException("Unnormalizable quaternion");

		norm = 1.0/Math.sqrt(norm);
		this.x = norm*q1.x;
		this.y = norm*q1.y;
		this.z = norm*q1.z;
		this.w = norm*q1.w;
	}


	/**
	 * Normalizes the value of this quaternion in place.
	 */
	public final void normalize()
	{
		double norm;

		norm = (this.x*this.x + this.y*this.y + this.z*this.z + this.w*this.w);
		if (norm == 0)
			throw new IllegalStateException("Unnormalizable quaternion");

		norm = 1.0 / Math.sqrt(norm);
		this.x *= norm;
		this.y *= norm;
		this.z *= norm;
		this.w *= norm;
	}

	
	/**
	 * Returns the rotation matrix corresponding to this unit quaternion. It
	 * is stored in row major order: [m00, m10, ...].
	 * 
	 * @return the rotation matrix 
	 */
	public final double[] getRotationMatrix() {
		double m00 = (1.0 - 2.0*y*y - 2.0*z*z);
		double m10 = (2.0*(x*y + w*z));
		double m20 = (2.0*(x*z - w*y));

		double m01 = (2.0*(x*y - w*z));
		double m11 = (1.0 - 2.0*x*x - 2.0*z*z);
		double m21 = (2.0*(y*z + w*x));

		double m02 = (2.0*(x*z + w*y));
		double m12 = (2.0*(y*z - w*x));
		double m22 = (1.0 - 2.0*x*x - 2.0*y*y);
		
		return new double[] {
				// this matrix is "visually transposed"
				m00, m10, m20, 0,
				m01, m11, m21, 0,
				m02, m12, m22, 0,
				0,   0,   0,   1,
		};
	}
	
	
	/**
	 * Sets the value of this quaternion to the rotation represented by the
	 * vector argument.  The direction of the vector represents the rotation
	 * axis, and its length represents the rotation angle in radians.
	 * 
	 * @param vx the x component of the rotation vector
	 * @param vy the y component of the rotation vector
	 * @param vz the z component of the rotation vector
	 */
	public final void setFromRotationVector(double vx, double vy, double vz) {
		double norm = Math.sqrt(vx*vx + vy*vy + vz*vz);
		double radians = norm/2;
		double n1 = norm == 0 ? 0 : Math.sin(radians)/norm;
		double n2 = Math.cos(radians);
		this.x = n1*vx;
		this.y = n1*vy;
		this.z = n1*vz;
		this.w = n2;
	}
	
	
	/**
	 * Sets the value of this quaternion to the rotational component of
	 * the passed matrix. The matrix elements are stored row major order
	 * inside the array: [m00, m10, ...].
	 * 
	 * @param matrix the matrix
	 */
	@SuppressWarnings("unused")
	public final void set(double[] matrix)
	{
		// this matrix is "visually transposed"
		double m00 = matrix[0],  m10 = matrix[1],  m20 = matrix[2],  m30 = matrix[3];
		double m01 = matrix[4],  m11 = matrix[5],  m21 = matrix[6],  m31 = matrix[7];
		double m02 = matrix[8],  m12 = matrix[9],  m22 = matrix[10], m32 = matrix[11];
		double m03 = matrix[12], m13 = matrix[13], m23 = matrix[14], m33 = matrix[15];
		
		double ww = 0.25*(m00 + m11 + m22 + m33);

		if (ww >= 0) {
			if (ww >= EPS2) {
				this.w = Math.sqrt(ww);
				ww = 0.25/this.w;
				this.x = (m21 - m12)*ww;
				this.y = (m02 - m20)*ww;
				this.z = (m10 - m01)*ww;
				return;
			}
		} else {
			this.w = 0;
			this.x = 0;
			this.y = 0;
			this.z = 1;
			return;
		}

		this.w = 0;
		ww = -0.5*(m11 + m22);
		if (ww >= 0) {
			if (ww >= EPS2){
				this.x =  Math.sqrt(ww);
				ww = 0.5/this.x;
				this.y = m10*ww;
				this.z = m20*ww;
				return;
			}
		} else {
			this.x = 0;
			this.y = 0;
			this.z = 1;
			return;
		}

		this.x = 0.0;
		ww = 0.5*(1.0 - m22);
		if (ww >= EPS2) {
			this.y =  Math.sqrt(ww);
			this.z = m21/(2.0*this.y);
			return;
		}

		this.y =  0;
		this.z =  1;
	}
	
	
	/**
	 *  Performs a great circle interpolation between this quaternion
	 *  and the quaternion parameter and places the result into this
	 *  quaternion.
	 *  @param q1  the other quaternion
	 *  @param alpha  the alpha interpolation parameter
	 */
	public final void interpolate(Quaternion q1, double alpha) {
		// From "Advanced Animation and Rendering Techniques"
		// by Watt and Watt pg. 364, function as implemented appeared to be
		// incorrect.  Fails to choose the same quaternion for the double
		// covering. Resulting in change of direction for rotations.
		// Fixed function to negate the first quaternion in the case that the
		// dot product of q1 and this is negative. Second case was not needed.
		double dot,s1,s2,om,sinom;

		dot = x*q1.x + y*q1.y + z*q1.z + w*q1.w;

		if ( dot < 0 ) {
			// negate quaternion
			q1.x = -q1.x;  q1.y = -q1.y;  q1.z = -q1.z;  q1.w = -q1.w;
			dot = -dot;
		}

		if ( (1.0 - dot) > EPS ) {
			om = Math.acos(dot);
			sinom = Math.sin(om);
			s1 = Math.sin((1.0-alpha)*om)/sinom;
			s2 = Math.sin( alpha*om)/sinom;
		} else{
			s1 = 1.0 - alpha;
			s2 = alpha;
		}

		w = s1*w + s2*q1.w;
		x = s1*x + s2*q1.x;
		y = s1*y + s2*q1.y;
		z = s1*z + s2*q1.z;
	}

	/**
	 *  Performs a great circle interpolation between quaternion q1
	 *  and quaternion q2 and places the result into this quaternion.
	 *  @param q1  the first quaternion
	 *  @param q2  the second quaternion
	 *  @param alpha  the alpha interpolation parameter
	 */
	public final void interpolate(Quaternion q1, Quaternion q2, double alpha) {
		// From "Advanced Animation and Rendering Techniques"
		// by Watt and Watt pg. 364, function as implemented appeared to be
		// incorrect.  Fails to choose the same quaternion for the double
		// covering. Resulting in change of direction for rotations.
		// Fixed function to negate the first quaternion in the case that the
		// dot product of q1 and this is negative. Second case was not needed.
		double dot,s1,s2,om,sinom;

		dot = q2.x*q1.x + q2.y*q1.y + q2.z*q1.z + q2.w*q1.w;

		if ( dot < 0 ) {
			// negate quaternion
			q1.x = -q1.x;  q1.y = -q1.y;  q1.z = -q1.z;  q1.w = -q1.w;
			dot = -dot;
		}

		if ( (1.0 - dot) > EPS ) {
			om = Math.acos(dot);
			sinom = Math.sin(om);
			s1 = Math.sin((1.0-alpha)*om)/sinom;
			s2 = Math.sin( alpha*om)/sinom;
		} else{
			s1 = 1.0 - alpha;
			s2 = alpha;
		}
		w = s1*q1.w + s2*q2.w;
		x = s1*q1.x + s2*q2.x;
		y = s1*q1.y + s2*q2.y;
		z = s1*q1.z + s2*q2.z;
	}

}

